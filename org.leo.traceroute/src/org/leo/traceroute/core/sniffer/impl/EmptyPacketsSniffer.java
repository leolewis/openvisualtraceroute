package org.leo.traceroute.core.sniffer.impl;

import org.leo.traceroute.core.ServiceFactory;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint;
import org.leo.traceroute.core.sniffer.AbstractPacketPoint.Protocol;
import org.leo.traceroute.core.sniffer.IPacketListener;
import org.leo.traceroute.core.sniffer.IPacketsSniffer;

import java.util.List;
import java.util.Set;

public class EmptyPacketsSniffer implements IPacketsSniffer {

    @Override
    public void startCapture(Set<Protocol> protocols, String port, boolean filterLenghtPackets, int length, String host, int captureTimeSeconds) {

    }

    @Override
    public void endCapture() {

    }

    @Override
    public void focus(AbstractPacketPoint point, boolean animation) {

    }

    @Override
    public void addListener(IPacketListener listener) {

    }

    @Override
    public void removeListener(IPacketListener listener) {

    }

    @Override
    public void clear() {

    }

    @Override
    public List<AbstractPacketPoint> getCapture() {
        return null;
    }

    @Override
    public void renotifyPackets() {

    }

    @Override
    public String toCSV() {
        return null;
    }

    @Override
    public String toText() {
        return null;
    }

    @Override
    public void init(ServiceFactory services) throws Exception {

    }

    @Override
    public void dispose() {

    }
}
