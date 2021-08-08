//package org.leo.traceroute.core.network;
//
//import org.apache.commons.lang3.tuple.Pair;
//import org.leo.traceroute.core.AbstractObject;
//import org.leo.traceroute.core.ServiceFactory;
//import org.leo.traceroute.core.ServiceFactory.Mode;
//import org.leo.traceroute.install.Env;
//import org.leo.traceroute.install.Env.OS;
//import org.pcap4j.core.*;
//import org.pcap4j.core.BpfProgram.BpfCompileMode;
//import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
//import org.pcap4j.packet.EthernetPacket;
//import org.pcap4j.packet.IpV4Packet;
//import org.pcap4j.packet.Packet;
//import org.pcap4j.packet.TcpPacket;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.net.InetAddress;
//import java.net.URL;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class Pcap4JNetworkService extends AbstractObject<INetworkInterfaceListener<?>> implements INetworkService<PcapNetworkInterface> {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(Pcap4JNetworkService.class);
//
//    private final static String TEST_URL = "www.google.com";
//
//    private final Map<Mode, List<PcapNetworkInterface>> _devices = new HashMap<>();
//    private final Map<PcapNetworkInterface, byte[]> _gatewayMac = new HashMap<>();
//
//    private final Map<Mode, Integer> _index = new HashMap<>();
//
//    @Override
//    public void init(ServiceFactory services) throws Exception {
//        List<PcapNetworkInterface> devices = Pcaps.findAllDevs();
//
//        // first time we use the network device, need to find the one connected
//        // to the Internet
//        if (Env.INSTANCE.getOs() != OS.mac) {
//            services.updateStartup("init.traceroute.network", true);
//            final InetAddress pingAddr = InetAddress.getByName(TEST_URL);
//            ExecutorService executor = null;
//            try {
//                executor = Executors.newFixedThreadPool(3);
//                final List<Pair<String, Future<Pair<PcapNetworkInterface, byte[]>>>> futures = new ArrayList<>();
//                for (int i = 0; i < devices.size(); i++) {
//                    final int fi = i;
//                    futures.add(Pair.of(devices.get(i).getName(), executor.submit(() -> {
//                        final PcapNetworkInterface netInterface = devices.get(fi);
//                        byte[] macAddress = netInterface.getLinkLayerAddresses().get(0).getAddress();
//                        LOGGER.info("Try interface " + netInterface.getName() + " " + netInterface.getDescription());
//                        try {
//                            final PcapHandle captor = netInterface.openLive(65535, PromiscuousMode.PROMISCUOUS, 2000);
//                            // obtain MAC address of the default gateway
//                            captor.setFilter("tcp and dst host " + pingAddr.getHostAddress(), BpfCompileMode.OPTIMIZE);
//                            byte[] getwayMac = null;
//                            int retry = 0;
//                            while (getwayMac == null) {
//                                new URL("http://" + TEST_URL).openStream().close();
//                                final PcapPacket ping = captor.getNextPacket();
//                                if (ping == null) {
//                                    if (retry++ >= 3) {
//                                        break;
//                                    }
//                                } else {
//                                    if (!Arrays.equals(ping.get(EthernetPacket.class).getHeader().getDstAddr().getAddress(), macAddress)) {
//                                        getwayMac = ping.get(EthernetPacket.class).getHeader().getDstAddr().getAddress();
//                                    }
//                                }
//                            }
//                            return Pair.of(netInterface, getwayMac);
//                        } catch (PcapNativeException e) {
//                            LOGGER.info("Interface " + netInterface.getDescription() + " is not usable, skip it", e);
//                            return null;
//                        }
//                    })));
//                }
//                for (final Pair<String, Future<Pair<PcapNetworkInterface, byte[]>>> f : futures) {
//                    try {
//                        final Pair<PcapNetworkInterface, byte[]> res = f.getValue().get(2000, TimeUnit.MILLISECONDS);
//                        if (res == null) {
//                            continue;
//                        }
//                        final byte[] getwayMac = res.getRight();
//                        final PcapNetworkInterface netInterface = res.getLeft();
//                        // a mac address for the default gateway
//                        if (getwayMac != null) {
//                            // interface is good to use for traceroute
//                            _devices.computeIfAbsent(Mode.TRACE_ROUTE, m -> new ArrayList<>()).add(netInterface);
//                            _gatewayMac.put(netInterface, getwayMac);
//                            LOGGER.info("Device {}({}) usable for TraceRoute", f.getValue(), f.getKey());
//                        }
//                        _devices.computeIfAbsent(Mode.SNIFFER, m -> new ArrayList<>()).add(netInterface);
//                        LOGGER.info("Device {}({}) usable for Sniffer", f.getValue(), f.getKey());
//                    } catch (final TimeoutException e) {
//                        // device is not usable
//                        LOGGER.warn("Device timed out {}({})", f.getValue(), f.getKey());
//                    }
//                }
//            } catch (final Throwable ex) {
//                LOGGER.warn("Cannot find a suitable network device for tracing.", ex);
//            } finally {
//                if (executor != null) {
//                    executor.shutdown();
//                }
//            }
//        }
//    }
//
//    @Override
//    public int getCurrentNetworkInterfaceIndex(Mode mode) {
//        return _index.getOrDefault(mode, 0);
//    }
//
//    /**
//     * List of network devices
//     *
//     * @return the list
//     */
//    @Override
//    public List<Pair<Integer, String>> getNetworkDevices(Mode mode) {
//        List<PcapNetworkInterface> devices = getDevices(mode);
//        final List<Pair<Integer, String>> list = new ArrayList<>();
//        for (int i = 0; i < devices.size(); i++) {
//            final PcapNetworkInterface net = devices.get(i);
//            final String text = net.getDescription() == null || net.getDescription().trim().length() == 0 ? net.getName() : (net.getDescription() + " (" + net.getName() + ")");
//            list.add(Pair.of(i, text));
//        }
//        return list;
//    }
//
//    /**
//     * Set the current network device
//     */
//    @Override
//    public void setCurrentNetworkDevice(Mode mode, final int deviceIndex) {
//        List<PcapNetworkInterface> devices = getDevices(mode);
//        if (!devices.isEmpty()) {
//            _index.put(mode, Math.min(Math.max(0, deviceIndex), devices.size() - 1));
//            notifyInterface(mode);
//        }
//    }
//
//    @Override
//    public void notifyInterface(Mode mode) {
//        List<PcapNetworkInterface> devices = getDevices(mode);
//        if (!devices.isEmpty()) {
//            for (final INetworkInterfaceListener listener : getListeners()) {
//                final PcapNetworkInterface net = devices.get(_index.getOrDefault(mode, 0));
//                listener.notifyNewNetworkInterface(net, mode, _gatewayMac.get(net));
//            }
//        }
//    }
//
//    @Override
//    public String getCurrentNetworkInterfaceName(Mode mode) {
//        List<PcapNetworkInterface> devices = getDevices(mode);
//        if (!devices.isEmpty()) {
//            return devices.get(_index.get(mode)).getName();
//        }
//        return null;
//    }
//
//    @Override
//    public void dispose() {
//
//    }
//
//    private List<PcapNetworkInterface> getDevices(Mode mode) {
//        return _devices.getOrDefault(mode, Collections.emptyList());
//    }
//}
