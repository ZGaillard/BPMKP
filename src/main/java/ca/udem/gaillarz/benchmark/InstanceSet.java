package ca.udem.gaillarz.benchmark;

import ca.udem.gaillarz.model.MKPInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple holder for a collection of instances.
 */
public class InstanceSet {
    private final String name;
    private final List<String> instanceNames;
    private final List<MKPInstance> instances;

    public InstanceSet(String name, List<String> instanceNames, List<MKPInstance> instances) {
        if (instanceNames.size() != instances.size()) {
            throw new IllegalArgumentException("Names and instances size mismatch");
        }
        this.name = name;
        this.instanceNames = new ArrayList<>(instanceNames);
        this.instances = new ArrayList<>(instances);
    }

    public String getName() {
        return name;
    }

    public int size() {
        return instances.size();
    }

    public MKPInstance getInstance(int index) {
        return instances.get(index);
    }

    public String getInstanceName(int index) {
        return instanceNames.get(index);
    }

    public List<MKPInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public List<String> getInstanceNames() {
        return Collections.unmodifiableList(instanceNames);
    }
}
