# JmDNS rules - refined to address broad keep warning
-keep class javax.jmdns.impl.DNSIncoming** { *; }
-keep class javax.jmdns.ServiceListener { *; }
-keep class javax.jmdns.ServiceInfo { *; }
-keep class javax.jmdns.JmDNS { *; }
-dontwarn javax.jmdns.**
