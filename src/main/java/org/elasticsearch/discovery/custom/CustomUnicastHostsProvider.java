package org.elasticsearch.discovery.custom;

import java.util.List;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastHostsProvider;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastZenPing;
import org.elasticsearch.transport.TransportService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.collect.Lists;
import com.netflix.elasticcar.identity.ElasticCarInstance;
import com.netflix.elasticcar.utils.DataFetcher;
import com.netflix.elasticcar.utils.EsUtils;

public class CustomUnicastHostsProvider extends AbstractComponent implements UnicastHostsProvider {
	private final TransportService transportService;
	private final Version version;
  
  @Inject
  public CustomUnicastHostsProvider(Settings settings, TransportService transportService, Version version) {
    super(settings);
    logger.info("&&& Inside CustomUnicastHostsProvider ......");
    this.transportService = transportService;
    this.version = version;
  }

  @Override
  public List<DiscoveryNode> buildDynamicNodes() {
		logger.info("&&& Inside buildDynamicNodes .....");
		List<DiscoveryNode> discoNodes = Lists.newArrayList();
		logger.info("&&& Before getting All Ids .....");
		// TODO: This should be equal to CassConfig.getCluster due to separate
		// injectors
		String strNodes = DataFetcher.fetchData("http://127.0.0.1:8080/Elasticcar/REST/v1/esconfig/get_nodes");
		try {
			List<ElasticCarInstance> instances = EsUtils.getEsCarInstancesFromJson((JSONObject) new JSONParser().parse(strNodes));
			if (instances == null)
				logger.info("Instances are NULL");
			if (instances.size() == 0)
				logger.info("Instance size is ZERO");
			for (ElasticCarInstance instance : instances) {
				try {
					logger.info("---Host Ip = " + instance.getHostIP());
					TransportAddress[] addresses = transportService.addressesFromString(instance.getHostIP());
					// we only limit to 1 addresses, makes no sense to ping 100
					// ports
					for (int i = 0; (i < addresses.length && i < UnicastZenPing.LIMIT_PORTS_COUNT); i++) {
						logger.info(
								"adding {}, address {}, transport_address {}",
								instance.getId(), instance.getHostIP(),addresses[i]);
						discoNodes.add(new DiscoveryNode(instance.getId(),addresses[i], version));
					}
				} catch (Exception e) {
					logger.warn("failed to add {}, address {}", e,instance.getId(), instance.getHostIP());
				}
			}
		} catch (Exception e) {
			logger.error("Caught an exception while trying to add buildDynamicNodes", e);
			throw new RuntimeException(e);
		}
    logger.info("using dynamic discovery nodes {}", discoNodes);

    return discoNodes;
  }
}
