package com.flyfox.util.ehcache;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EhCacheProvider {

	public static void main(String[] args) {
		EhCacheProvider provider = new EhCacheProvider();
		provider.start();
		EhCache cache = provider.buildCache("session", true);
		cache.put("123", "abc");
		System.out.println(cache.get("123"));
		provider.stop();
	}
	
	private final static Logger log = LoggerFactory.getLogger(EhCacheProvider.class);
	private final static String CONFIG_XML = "/ehcache.xml";

	private CacheManager manager;
	private ConcurrentHashMap<String, EhCache> _CacheManager;

	public EhCache buildCache(String name, boolean autoCreate) {
		EhCache ehcache = _CacheManager.get(name);
		if (ehcache == null && autoCreate) {
			synchronized (_CacheManager) {
				ehcache = _CacheManager.get(name);
				if (ehcache == null) {
					Cache cache = manager.getCache(name);
					if (cache == null) {
						log.warn("Could not find configuration [" + name + "]; using defaults.");
						manager.addCache(name);
						cache = manager.getCache(name);
						log.debug("started EHCache region: " + name);
					}
					ehcache = new EhCache(cache);
					_CacheManager.put(name, ehcache);
				}
			}
		}
		return ehcache;
	}

	public void start() {
		if (manager != null) {
			log.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() "
					+ " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider."
					+ " If this behaviour is required, consider using net.sf.ehcache.hibernate.SingletonEhCacheProvider.");
			return;
		}
		URL xml = getClass().getClassLoader().getParent().getResource(CONFIG_XML);
		if (xml == null)
			xml = getClass().getResource(CONFIG_XML);
		if (xml == null)
			throw new RuntimeException("cannot find ehcache.xml !!!");

		manager = new CacheManager();
		_CacheManager = new ConcurrentHashMap<String, EhCache>();
	}

	public void stop() {
		if (manager != null) {
			manager.shutdown();
			manager = null;
		}
	}

}