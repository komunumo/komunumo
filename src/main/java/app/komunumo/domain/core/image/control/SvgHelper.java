/*
 * Komunumo - Open Source Community Manager
 * Copyright (C) Marcus Fihlon and the individual contributors to Komunumo.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package app.komunumo.domain.core.image.control;

import app.komunumo.KomunumoException;
import app.komunumo.util.ImageUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

final class SvgHelper {

    // Store the user SVG as a string (without <svg> element)
    private final String userSvgString;

    private final double userSvgWidth;
    private final double userSvgHeight;

    private static final String SPLIT_MARKE_STRING = "___USVG__";

    SvgHelper(final @NotNull String instanceLogo) {
        try {
            final var inputStream = new ByteArrayInputStream(instanceLogo.getBytes(StandardCharsets.UTF_8));
            final var svgDocument = parseSvg(inputStream, true);
            final var svgElement = svgDocument.getDocumentElement();

            userSvgString = stripSvgShellElement(svgDocument);
            userSvgWidth = deriveSvgDimension(svgElement, "width");
            userSvgHeight = deriveSvgDimension(svgElement, "height");
        } catch (final Exception e) {
            throw new KomunumoException("Failed to initialize template parser: " + e.getMessage(), e);
        }
    }

    double getUserSvgWidth() {
        return userSvgWidth;
    }

    double getUserSvgHeight() {
        return userSvgHeight;
    }

    /*
     * Parse, validate and prepare the template wrapper SVG around the "Logo" group
     */
    String parseTemplate(final @NotNull String wrapperSvg) {
        try {
            final var document = parseSvg(wrapperSvg, false);

            final var xPath = XPathFactory.newInstance().newXPath();
            final var xpathExpr = "//%s[@id='%s']".formatted("g", "Logo");
            final var gLogo = (Element) xPath.evaluate(xpathExpr, document, XPathConstants.NODE);

            if (gLogo == null) {
                throw new KomunumoException("Container element not found");
            }

            while (gLogo.hasChildNodes()) {
                gLogo.removeChild(gLogo.getFirstChild());
            }
            gLogo.appendChild(document.createTextNode(SPLIT_MARKE_STRING));

            final var writer = new StringWriter();
            final var transformer = newHardenedTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (final Exception e) {
            throw new KomunumoException("Failed to parse SVG template: " + e.getMessage(), e);
        }
    }

    /*
     * Apply the applyable(expanded) template string by wrapping it around custom SVG content
     */
    String applyTemplate(final @NotNull String preppedTemplate) {
        return preppedTemplate.replace(SPLIT_MARKE_STRING, userSvgString);
    }

    // Parse the SVG and extract its inner contents, stripping away the <svg> wrapper
    private String stripSvgShellElement(final @NotNull Document doc) throws Exception {
        final var svgContent = new StringBuilder();
        final var rootElement = doc.getDocumentElement();

        final Transformer transformer = newHardenedTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        final var children = rootElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final var child = children.item(i);
            final var writer = new StringWriter();
            transformer.transform(new DOMSource(child), new StreamResult(writer));
            svgContent.append(writer);
        }

        return svgContent.toString();
    }

    /**
     *  Builds a hardened Transformer that avoids any external access (DTD/XSL).
     */
    private static Transformer newHardenedTransformer() throws TransformerConfigurationException {
        return newHardenedTransformer(TransformerFactory.newInstance());
    }

    /**
     *  Testable seam: harden a provided TransformerFactory instance.
     */
    @VisibleForTesting
    static Transformer newHardenedTransformer(final TransformerFactory tf) throws TransformerConfigurationException {
        // Enable secure processing (limits expansion, time, memory; implementation-dependent)
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        // Forbid external DTDs and stylesheets (prevents network/file access)
        try {
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ignored) {
            // Some TransformerFactory implementations may not support these attributes.
        }

        final Transformer transformer = tf.newTransformer();

        // Never resolve URIs in XSLT (extra safety even though we use no stylesheets)
        transformer.setURIResolver((_, _) -> null);

        return transformer;
    }

    /**
     * <p>Parses an SVG from the given {@link InputStream} into a DOM {@link Document}.</p>
     *
     * <p>This method is configured to accept DOCTYPE declarations in the SVG,
     * but prevents the parser from loading any external DTDs or schemas.
     * Network requests (e.g., to w3.org) are disabled by:</p>
     *
     * <ul>
     *   <li>Disallowing access to external DTDs and schemas via JAXP properties</li>
     *   <li>Disabling the loading of external DTDs</li>
     *   <li>Providing a no-op {@link org.xml.sax.EntityResolver} to resolve entities locally</li>
     * </ul>
     *
     * <p>This ensures the SVG can be parsed without validation and without
     * triggering HTTP requests, while still producing a usable DOM tree.</p>
     *
     * @param inputStream the input stream containing the SVG content; must not be {@code null}
     * @return the parsed SVG as a DOM {@link Document}; will never be {@code null}
     * @throws Exception if parsing fails for any reason
     */
    @SuppressWarnings("HttpUrlsUsage")
    @VisibleForTesting
    static @NotNull Document parseSvg(final @NotNull InputStream inputStream,
                                      final boolean namespaceAwareness) throws Exception {
        final var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(namespaceAwareness);
        factory.setValidating(false); // no validation needed

        // Prevent any external access for DTDs and XML Schemas
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        // Do not load external DTDs (avoids network calls to w3.org)
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        // Allow DOCTYPE declarations (explicitly set to be clear)
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);

        final var builder = factory.newDocumentBuilder();

        // No-op EntityResolver: if a DOCTYPE systemId is present, nothing will be loaded
        builder.setEntityResolver(noOpEntityResolver());

        return builder.parse(inputStream);
    }

    /**
     *  Returns a no-op EntityResolver that never fetches external resources.
     */
    @VisibleForTesting
    static @NotNull EntityResolver noOpEntityResolver() {
        return (_, _) -> new InputSource(Reader.of(""));
    }

    /**
     * Parses an SVG from the given String into a DOM {@link Document}.
     * Delegates to {@link #parseSvg(InputStream,boolean)} to avoid code duplication.
     */
    static @NotNull Document parseSvg(final @NotNull String svg,
                                      final boolean namespaceAwareness) throws Exception {
        try (var in = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8))) {
            return parseSvg(in, namespaceAwareness);
        }
    }

    // Parse the SVG dimension and convert it to pixels (handling various units)
    double deriveSvgDimension(final @NotNull Element docElement,
                              final @NotNull String dimensionType) {
        final var dimension = docElement.getAttribute(dimensionType);

        // If width/height is not provided, fall back to the viewBox values
        //noinspection ConstantValue (dimension can be null)
        if (dimension == null || dimension.isBlank()) {
            return getDimensionFromViewBox(docElement, dimensionType);
        }

        // Handle % unit (percentage of the viewBox)
        double referenceValue = 0;
        if (dimension.endsWith("%")) {
            referenceValue = getDimensionFromViewBox(docElement, dimensionType);
        }

        // Handle different units like in, mm, cm, pt, pc
        return ImageUtil.convertToPixels(dimension, referenceValue);
    }

    // Extract the width/height from the viewBox if width/height are not provided
    private double getDimensionFromViewBox(final @NotNull Element docElement,
                                           final @NotNull String dimensionType) {
        final var viewBox = docElement.getAttribute("viewBox");
        //noinspection ConstantValue (viewbox can be null)
        if (viewBox == null || viewBox.isBlank()) {
            throw new IllegalArgumentException("No viewBox or dimensions found in SVG.");
        }

        final var viewBoxParts = viewBox.split(" ");
        if (viewBoxParts.length < 4) {
            throw new IllegalArgumentException("Invalid viewBox attribute in SVG.");
        }

        // Return the appropriate dimension from the viewBox (width or height)
        if ("width".equals(dimensionType)) {
            return Double.parseDouble(viewBoxParts[2]);
        } else if ("height".equals(dimensionType)) {
            return Double.parseDouble(viewBoxParts[3]);
        }

        return 0;
    }

}
