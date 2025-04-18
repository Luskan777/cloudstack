/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import com.cloud.host.Host;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VirtualMachine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceSkipThreshold;
import static org.apache.cloudstack.cluster.ClusterDrsService.ClusterDrsImbalanceThreshold;

public class Condensed extends AdapterBase implements ClusterDrsAlgorithm {

    private static final Logger logger = LogManager.getLogger(Condensed.class);

    @Override
    public boolean needsDrs(Cluster cluster, List<Ternary<Long, Long, Long>> cpuList,
                            List<Ternary<Long, Long, Long>> memoryList) throws ConfigurationException {
        long clusterId = cluster.getId();
        double threshold = getThreshold(clusterId);
        Float skipThreshold = ClusterDrsImbalanceSkipThreshold.valueIn(clusterId);
        Double imbalance = ClusterDrsAlgorithm.getClusterImbalance(clusterId, cpuList, memoryList, skipThreshold);
        String drsMetric = ClusterDrsAlgorithm.getClusterDrsMetric(clusterId);
        String metricType = ClusterDrsAlgorithm.getDrsMetricType(clusterId);
        Boolean useRatio = ClusterDrsAlgorithm.getDrsMetricUseRatio(clusterId);
        if (imbalance < threshold) {

            logger.debug("Cluster {} needs DRS. Imbalance: {} Threshold: {} Algorithm: {} DRS metric: {} Metric Type: {} Use ratio: {} SkipThreshold: {}",
                    cluster, imbalance, threshold, getName(), drsMetric, metricType, useRatio, skipThreshold);
            return true;
        } else {
            logger.debug("Cluster {} does not need DRS. Imbalance: {} Threshold: {} Algorithm: {} DRS metric: {} Metric Type: {} Use ratio: {} SkipThreshold: {}",
                    cluster, imbalance, threshold, getName(), drsMetric, metricType, useRatio, skipThreshold);
            return false;
        }
    }

    private double getThreshold(long clusterId) {
        return ClusterDrsImbalanceThreshold.valueIn(clusterId);
    }

    @Override
    public String getName() {
        return "condensed";
    }

    @Override
    public Ternary<Double, Double, Double> getMetrics(Cluster cluster, VirtualMachine vm,
            ServiceOffering serviceOffering, Host destHost,
            Map<Long, Ternary<Long, Long, Long>> hostCpuMap, Map<Long, Ternary<Long, Long, Long>> hostMemoryMap,
            Boolean requiresStorageMotion) throws ConfigurationException {
        Double preImbalance = ClusterDrsAlgorithm.getClusterImbalance(cluster.getId(), new ArrayList<>(hostCpuMap.values()),
                new ArrayList<>(hostMemoryMap.values()), null);
        Double postImbalance = getImbalancePostMigration(serviceOffering, vm, destHost, hostCpuMap, hostMemoryMap);

        logger.debug("Cluster {} pre-imbalance: {} post-imbalance: {} Algorithm: {} VM: {} srcHost: {} destHost: {}",
                cluster, preImbalance, postImbalance, getName(), vm, vm.getHostId(), destHost);

        // This needs more research to determine the cost and benefit of a migration
        // TODO: Cost should be a factor of the VM size and the host capacity
        // TODO: Benefit should be a factor of the VM size and the host capacity and the number of VMs on the host
        final double improvement = postImbalance - preImbalance;
        final double cost = 0;
        final double benefit = 1;
        return new Ternary<>(improvement, cost, benefit);
    }
}
