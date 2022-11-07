/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.kelp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class XmlLoader {
    private static final String ID_ATTRIBUTE = "id";
    private static final String MINECRAFT_PREFIX = "minecraft:";

    public static ContentKinds load(Path path) {
        Document document;

        try (InputStream in = Files.newInputStream(path)) {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException("Could not parse XML", e);
        }

        Element root = document.getDocumentElement();
        Map<String, Map<String, String>> data = new HashMap<>();

        for (Node node : asIterable(root.getChildNodes())) {
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element child = (Element) node;

            String id = convertToTranslationKey(child.getAttribute(ID_ATTRIBUTE));
            String translation = guessTranslationValue(id);
            data.computeIfAbsent(child.getTagName(), n -> new LinkedHashMap<>())
                .put(id, translation);
        }

        return new ContentKinds(data);
    }

    private static String convertToTranslationKey(String id) {
        if (id.startsWith(MINECRAFT_PREFIX)) {
            id = id.substring(MINECRAFT_PREFIX.length());
        }

        return id.replace(':', '.');
    }

    private static String guessTranslationValue(String key) {
        int dotIndex = key.lastIndexOf('.');

        if (dotIndex != -1) {
            key = key.substring(dotIndex + 1);
        }

        String[] words = key.split("_");
        return Stream.of(words)
            .map(s -> {
                if (s.length() == 1) {
                    return s.toUpperCase(Locale.ROOT);
                }

                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
            })
            .collect(Collectors.joining(" "));
    }

    private static Iterable<Node> asIterable(NodeList nodeList) {
        return () -> createNodeListIterator(nodeList);
    }

    private static Iterator<Node> createNodeListIterator(NodeList nodeList) {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < nodeList.getLength();
            }

            @Override
            public Node next() {
                return nodeList.item(index++);
            }
        };
    }
}
