/*
 * Copyright (C) 2017 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-solr-modules-replica-searcher
 *
 * dbc-solr-modules-replica-searcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-solr-modules-replica-searcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.solr.module.searcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.solr.cloud.CloudDescriptor;
import org.apache.solr.cloud.ZkController;
import org.apache.solr.common.cloud.ClusterState;
import org.apache.solr.common.cloud.DocCollection;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchHandler;
import org.apache.solr.handler.component.ShardHandlerFactory;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author DBC {@literal <dbc.dk>}
 */
public class ReplicaSearcher extends SearchHandler {

    private static final Logger log = LoggerFactory.getLogger(ReplicaSearcher.class);

    private ShardHandlerFactory shardHandlerFactory;
    private PluginInfo shfInfo;

    public ReplicaSearcher() {
        this.shardHandlerFactory = null;
        this.shfInfo = null;
    }

    @Override
    public void init(PluginInfo info) {
        super.init(info);
        log.info("INIT DBC");

        for (PluginInfo child : info.children) {
            if ("shardHandlerFactory".equals(child.type)) {
                this.shfInfo = child;
                break;
            }
        }
    }

    @Override
    public void inform(SolrCore core) {
        super.inform(core);
        log.info("INFORM DBC");

        if (shfInfo == null) {
            shardHandlerFactory = core
                    .getCoreContainer().getShardHandlerFactory();
        } else {
            shardHandlerFactory = core.createInitInstance(shfInfo, ShardHandlerFactory.class, null, null);
            core.addCloseHook(new CloseHook() {
                @Override
                public void preClose(SolrCore core) {
                    shardHandlerFactory.close();
                }

                @Override
                public void postClose(SolrCore core) {
                }
            });
        }
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        if (req.getParams().get(ShardParams.SHARDS) == null) {
            SolrCore core = req.getCore();
            CoreDescriptor coreDescriptor = core.getCoreDescriptor();

            CoreContainer coreContainer = core.getCoreContainer();
            boolean isZkAware = coreContainer.isZooKeeperAware();
            boolean isDistrib = req.getParams().getBool("distrib", isZkAware);
            if (isZkAware && isDistrib) {

                ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());

                String q = params.get(CommonParams.Q, "");
                FNV1 hash = FNV1.hash(q);

                CloudDescriptor cloudDescriptor = coreDescriptor.getCloudDescriptor();
                ZkController zkController = coreContainer.getZkController();
                ClusterState clusterState = zkController.getClusterState();

                String collectionNames = params.get("collection");
                if (collectionNames == null) {
                    collectionNames = cloudDescriptor.getCollectionName();
                }

                ArrayList<String> shards = new ArrayList<>();

                Arrays.stream(collectionNames.split(","))
                        .forEach(collectionName -> {
                            hash.reset();
                            DocCollection collection = clusterState.getCollection(collectionName);
                            collection.getSlicesMap().entrySet().stream()
                                    .sorted(Map.Entry.comparingByKey())
                                    .map(Map.Entry::getValue)
                                    .forEachOrdered(slice -> {
                                        List<Replica> replicas = slice.getReplicasMap().entrySet().stream()
                                                .sorted(Map.Entry.comparingByKey())
                                                .map(Map.Entry::getValue)
                                                .filter(r -> r.getState() == Replica.State.ACTIVE && clusterState.liveNodesContain(r.getNodeName()))
                                                .collect(Collectors.toList());
                                        if (!replicas.isEmpty()) {
                                            long offset = hash.take(replicas.size());
                                            shards.add(replicas.get((int) offset).getCoreUrl());
                                        }
                                    });
                        });
                log.debug("shards = " + shards);
                params.set(ShardParams.SHARDS, String.join(",", shards));
                req.setParams(params);
            }
        }
        super.handleRequestBody(req, rsp);
    }

}
