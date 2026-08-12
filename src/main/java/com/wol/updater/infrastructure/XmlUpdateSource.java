package com.wol.updater.infrastructure;

import com.wol.updater.application.UpdateSource;
import com.wol.updater.domain.DownloadPackage;
import com.wol.updater.domain.UpdatePlan;
import com.wol.updater.domain.VersionSignature;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class XmlUpdateSource implements UpdateSource {

    private static final String PRIMARY_URL = "http://aoe3wol.com/updates/UpdateInfo.xml";
    private static final String FALLBACK_URL = "http://master.dl.sourceforge.net/project/wars-of-liberty/Patches/UpdateInfo.xml";

    private final HttpClient httpClient;

    public XmlUpdateSource() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Optional<UpdatePlan> getUpdatePlan(VersionSignature localSignature) {
        Optional<String> xmlContent = fetchContent(PRIMARY_URL);
        if (xmlContent.isEmpty()) {
            xmlContent = fetchContent(FALLBACK_URL);
        }

        return xmlContent.flatMap(xml -> buildPlanFromXml(xml, localSignature));
    }

    private Optional<String> fetchContent(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Optional.of(response.body());
            }
        } catch (Exception e) {
            // Log failure or return empty
        }
        return Optional.empty();
    }

    private Optional<UpdatePlan> buildPlanFromXml(String xml, VersionSignature localSignature) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            ManifestHandler handler = new ManifestHandler();
            
            java.io.ByteArrayInputStream input = new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            parser.parse(input, handler);

            String currentVersion = handler.versionMap.get(localSignature);
            if (currentVersion == null) {
                return Optional.empty(); // Unknown local version
            }

            Integer minReqDownloadId = handler.minDownloadMap.get(currentVersion);
            if (minReqDownloadId == null) {
                return Optional.empty(); // Should not happen if XML is well-formed
            }

            // Determine which packages to download
            List<DownloadPackage> requiredPackages = new ArrayList<>();
            if (minReqDownloadId > 0) {
                requiredPackages = handler.availablePackages.stream()
                        .filter(pkg -> pkg.id() >= minReqDownloadId)
                        .sorted(Comparator.comparingInt(DownloadPackage::id))
                        .collect(Collectors.toList());
            }

            // Target version is the version of the most recent package required, or current if none required.
            // Since it's sorted ascending, the newest package is at the end of the list.
            String targetVersion = requiredPackages.isEmpty() ? currentVersion : requiredPackages.get(requiredPackages.size() - 1).targetVersion();

            return Optional.of(new UpdatePlan(targetVersion, currentVersion, requiredPackages));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static class ManifestHandler extends DefaultHandler {
        Map<VersionSignature, String> versionMap = new HashMap<>();
        Map<String, Integer> minDownloadMap = new HashMap<>();
        List<DownloadPackage> availablePackages = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("version".equalsIgnoreCase(qName)) {
                String tmpVer = attributes.getValue("ver");
                String tmpTech = attributes.getValue("techmd5");
                String tmpStr = attributes.getValue("strmd5");
                String tmpPrt = attributes.getValue("protomd5");
                String tmpMinStr = attributes.getValue("minreqdownload");

                if (tmpVer != null && tmpTech != null && tmpStr != null && tmpPrt != null && tmpMinStr != null) {
                    VersionSignature sig = new VersionSignature(tmpStr, tmpTech, tmpPrt);
                    versionMap.put(sig, tmpVer);
                    minDownloadMap.put(tmpVer, Integer.parseInt(tmpMinStr));
                }
            } else if ("download".equalsIgnoreCase(qName)) {
                String tmpIdStr = attributes.getValue("id");
                String tmpSizeStr = attributes.getValue("size");
                String tmpCrcStr = attributes.getValue("crc32");
                String tmpLink = attributes.getValue("link");
                String tmpAltLink = attributes.getValue("alt");
                String tmpDeleteList = attributes.getValue("deleteList");
                String tmpVersion = attributes.getValue("version");
                String tmpPage = attributes.getValue("postUpdatePage");

                if (tmpIdStr != null && tmpVersion != null && tmpLink != null && tmpCrcStr != null && tmpSizeStr != null) {
                    availablePackages.add(new DownloadPackage(
                            Integer.parseInt(tmpIdStr),
                            Long.parseLong(tmpSizeStr),
                            tmpCrcStr,
                            tmpLink,
                            Optional.ofNullable(tmpAltLink),
                            tmpVersion,
                            Optional.ofNullable(tmpDeleteList),
                            Optional.ofNullable(tmpPage)
                    ));
                }
            }
        }
    }
}
