package org.leo.traceroute.core.network;

import org.apache.commons.lang3.tuple.Pair;
import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.ServiceFactory.Mode;

import java.util.Collections;
import java.util.List;

public class EmptyNetworkService implements INetworkService<Void> {

    @Override
    public List<Pair<Integer, String>> getNetworkDevices(Mode mode) {
        return Collections.emptyList();
    }

    @Override
    public void setCurrentNetworkDevice(Mode mode, int deviceIndex) {

    }

    @Override
    public void notifyInterface(Mode mode) {

    }

    @Override
    public String getCurrentNetworkInterfaceName(Mode mode) {
        return "OS";
    }

    @Override
    public int getCurrentNetworkInterfaceIndex(Mode mode) {
        return 0;
    }

    @Override
    public void removeListener(INetworkInterfaceListener listener) {

    }

    @Override
    public void addListener(INetworkInterfaceListener listener) {

    }

    @Override
    public void init(ServiceFactory services) throws Exception {

    }

    @Override
    public void dispose() {

    }
}
