package redis.clients.jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Pool;

/**
 * 根据jedis源码：redis.clients.jedis.JedisSentinelPool更改而来，更改了以下地方：<br>
 * 1：不必指定masterName，会自动发现（不一样会抛异常）<br>
 * 2：切换master的时候，加上锁（因为有多个Sentinel的情况，会收到多次通知）<br>
 * 
 * @author jfan 2016年10月28日 下午5:49:27
 */
public class AutoJedisSentinelPool extends Pool<Jedis> {

  private static final Logger log = LoggerFactory.getLogger(AutoJedisSentinelPool.class);

  protected GenericObjectPoolConfig poolConfig;

  protected int connectionTimeout = Protocol.DEFAULT_TIMEOUT;
  protected int soTimeout = Protocol.DEFAULT_TIMEOUT;

  protected String password;

  protected int database = Protocol.DEFAULT_DATABASE;

  protected String clientName;

  protected Set<MasterListener> masterListeners = new HashSet<MasterListener>();

  private Object lock = new Object();// static ? volatile ?
  private volatile JedisFactory factory;
  private volatile HostAndPort currentHostMaster;

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig) {
    this(sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public AutoJedisSentinelPool(Set<String> sentinels) {
    this(sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, null,
        Protocol.DEFAULT_DATABASE);
  }

  public AutoJedisSentinelPool(Set<String> sentinels, String password) {
    this(sentinels, new GenericObjectPoolConfig(), Protocol.DEFAULT_TIMEOUT, password);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password) {
    this(sentinels, poolConfig, timeout, password, Protocol.DEFAULT_DATABASE);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout) {
    this(sentinels, poolConfig, timeout, null, Protocol.DEFAULT_DATABASE);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final String password) {
    this(sentinels, poolConfig, Protocol.DEFAULT_TIMEOUT, password);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database) {
    this(sentinels, poolConfig, timeout, timeout, password, database);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, int timeout, final String password,
      final int database, final String clientName) {
    this(sentinels, poolConfig, timeout, timeout, password, database, clientName);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int timeout, final int soTimeout,
      final String password, final int database) {
    this(sentinels, poolConfig, timeout, soTimeout, password, database, null);
  }

  public AutoJedisSentinelPool(Set<String> sentinels,
      final GenericObjectPoolConfig poolConfig, final int connectionTimeout, final int soTimeout,
      final String password, final int database, final String clientName) {
    this.poolConfig = poolConfig;
    this.connectionTimeout = connectionTimeout;
    this.soTimeout = soTimeout;
    this.password = password;
    this.database = database;
    this.clientName = clientName;

    HostAndPort master = initSentinels(sentinels);
    initPool(master);
  }

  public void destroy() {
    for (MasterListener m : masterListeners) {
      m.shutdown();
    }

    super.destroy();
  }

  public HostAndPort getCurrentHostMaster() {
    return currentHostMaster;
  }

  private void initPool(HostAndPort master) {
    if (!master.equals(currentHostMaster)) {
      currentHostMaster = master;
      if (factory == null) {
        factory = new JedisFactory(master.getHost(), master.getPort(), connectionTimeout,
            soTimeout, password, database, clientName, false, null, null, null);
        initPool(poolConfig, factory);
      } else {
        factory.setHostAndPort(currentHostMaster);
        // although we clear the pool, we still have to check the
        // returned object
        // in getResource, this call only clears idle instances, not
        // borrowed instances
        internalPool.clear();
      }

      log.info("Created JedisPool to master at " + master);
    }
  }

  private HostAndPort initSentinels(Set<String> sentinels) {

    String masterName = null;
    HostAndPort master = null;
    boolean sentinelAvailable = false;

    log.info("Trying to find master from available Sentinels...");

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);

      log.info("Connecting to Sentinel " + hap);

      Jedis jedis = null;
      try {
        jedis = new Jedis(hap.getHost(), hap.getPort());

        List<Map<String, String>> sentinelMasters = jedis.sentinelMasters();

        // connected to sentinel...
        sentinelAvailable = true;

        if (CollectionUtils.isEmpty(sentinelMasters))
          // throw new JedisException("Can be connected to the outpost, but no available redis master...");
          continue;
        if (1 != sentinelMasters.size())
          throw new JedisException("Can be connected to the outpost"
              + ", but found multiple of redis master, current model only supports one masterNode");

        Map<String, String> info = sentinelMasters.get(0);
        String name = info.get("name");
        if (null == masterName)
          masterName = name;
        if (!(masterName.equals(name)))
          throw new JedisException("Can be connected to the outpost"
              + ", but found multiple of masterName, Current is '" + masterName + "', Incoming is '" + name + "'");

        HostAndPort hp = new HostAndPort(info.get("ip"), Integer.parseInt(info.get("port")));
        if (null == master) {
          log.info("Found Redis master at " + hp);
          master = hp;
        }
        if (!(master.equals(hp)))
          throw new JedisException("Can be connected to the outpost"
              + ", but found different masterNode, Current is '" + master + "', Incoming is '" + hp + "'");
      } catch (JedisException e) {
        // resolves #1036, it should handle JedisException there's another chance
        // of raising JedisDataException
        log.warn("Cannot get master address from sentinel running @ " + hap + ". Reason: " + e
            + ". Trying next one.");
      } finally {
        if (jedis != null) {
          jedis.close();
        }
      }
    }

    if (master == null) {
      if (sentinelAvailable) {
        // can connect to sentinel, but master name seems to not
        // monitored
        throw new JedisException("Can connect to sentinel, but " + masterName
            + " seems to be not monitored...");
      } else {
        throw new JedisConnectionException("All sentinels down, cannot determine where is "
            + masterName + " master is running...");
      }
    }

    log.info("Redis master running at " + master + ", starting Sentinel listeners...");

    for (String sentinel : sentinels) {
      final HostAndPort hap = HostAndPort.parseString(sentinel);
      MasterListener masterListener = new MasterListener(masterName, hap.getHost(), hap.getPort());
      // whether MasterListener threads are alive or not, process can be stopped
      masterListener.setDaemon(true);
      masterListeners.add(masterListener);
      masterListener.start();
    }

    return master;
  }

  private HostAndPort toHostAndPort(List<String> getMasterAddrByNameResult) {
    String host = getMasterAddrByNameResult.get(0);
    int port = Integer.parseInt(getMasterAddrByNameResult.get(1));

    return new HostAndPort(host, port);
  }

  @Override
  public Jedis getResource() {
    while (true) {
      Jedis jedis = super.getResource();
      jedis.setDataSource(this);

      // get a reference because it can change concurrently
      final HostAndPort master = currentHostMaster;
      final HostAndPort connection = new HostAndPort(jedis.getClient().getHost(), jedis.getClient()
          .getPort());

      if (master.equals(connection)) {
        // connected to the correct master
        return jedis;
      } else {
        returnBrokenResource(jedis);
      }
    }
  }

  /**
   * @deprecated starting from Jedis 3.0 this method will not be exposed. Resource cleanup should be
   *             done using @see {@link redis.clients.jedis.Jedis#close()}
   */
  @Override
  @Deprecated
  public void returnBrokenResource(final Jedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  /**
   * @deprecated starting from Jedis 3.0 this method will not be exposed. Resource cleanup should be
   *             done using @see {@link redis.clients.jedis.Jedis#close()}
   */
  @Override
  @Deprecated
  public void returnResource(final Jedis resource) {
    if (resource != null) {
      resource.resetState();
      returnResourceObject(resource);
    }
  }

  protected class MasterListener extends Thread {

    protected String masterName;
    protected String host;
    protected int port;
    protected long subscribeRetryWaitTimeMillis = 5000;
    protected volatile Jedis j;
    protected AtomicBoolean running = new AtomicBoolean(false);

    protected MasterListener() {
    }

    public MasterListener(String masterName, String host, int port) {
      super(String.format("MasterListener-%s-[%s:%d]", masterName, host, port));
      this.masterName = masterName;
      this.host = host;
      this.port = port;
    }

    public MasterListener(String masterName, String host, int port,
        long subscribeRetryWaitTimeMillis) {
      this(masterName, host, port);
      this.subscribeRetryWaitTimeMillis = subscribeRetryWaitTimeMillis;
    }

    @Override
    public void run() {

      running.set(true);

      while (running.get()) {

        j = new Jedis(host, port);

        try {
          // double check that it is not being shutdown
          if (!running.get()) {
            break;
          }

          j.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              log.info("Sentinel " + host + ":" + port + " published: " + message + ".");

              String[] switchMasterMsg = message.split(" ");

              if (switchMasterMsg.length > 3) {

                if (masterName.equals(switchMasterMsg[0])) {
                  synchronized(lock) {
                    HostAndPort master = toHostAndPort(Arrays.asList(switchMasterMsg[3], switchMasterMsg[4]));
                    if (!(currentHostMaster.equals(master)))
                      initPool(master);
                  }
                } else {
                  log.debug("Ignoring message on +switch-master for master name "
                      + switchMasterMsg[0] + ", our master name is " + masterName);
                }

              } else {
                log.error("Invalid message received on Sentinel " + host + ":" + port
                    + " on channel +switch-master: " + message);
              }
            }
          }, "+switch-master");

        } catch (JedisConnectionException e) {

          if (running.get()) {
            log.error("Lost connection to Sentinel at " + host + ":" + port
                + ". Sleeping 5000ms and retrying.", e);
            try {
              Thread.sleep(subscribeRetryWaitTimeMillis);
            } catch (InterruptedException e1) {
              log.error("Sleep interrupted: ", e1);
            }
          } else {
            log.info("Unsubscribing from Sentinel at " + host + ":" + port);
          }
        } finally {
          j.close();
        }
      }
    }

    public void shutdown() {
      try {
        log.info("Shutting down listener on " + host + ":" + port);
        running.set(false);
        // This isn't good, the Jedis object is not thread safe
        if (j != null) {
          j.disconnect();
        }
      } catch (Exception e) {
        log.error("Caught exception while shutting down: ", e);
      }
    }
  }
}