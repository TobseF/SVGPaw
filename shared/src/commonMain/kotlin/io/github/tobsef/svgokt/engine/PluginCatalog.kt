package io.github.tobsef.svgokt.engine

/**
 * The SVGO plugin catalogue the UI is built from.
 *
 * Mirrors SVGOMG's `config.json` so identical settings give identical output, and keeps the
 * catalogue order — which is the order the plugins actually run in. The UI grouping in
 * [PluginCategory] is purely cosmetic and never affects execution order.
 */

/** A display group in the features panel. */
public data class PluginCategory(
    val key: String,
    val label: String,
    val blurb: String,
)

/** One SVGO plugin as the UI knows it. */
public data class PluginInfo(
    val id: String,
    val category: String,
    val label: String,
    val help: String,
    /** Enabled out of the box exactly when the plugin is part of SVGO 4.1's `preset-default`. */
    val defaultEnabled: Boolean,
)

public val CATEGORIES: List<PluginCategory> = listOf(
    PluginCategory(
        "document", "Document & metadata",
        "Boilerplate and editor leftovers that no renderer needs.",
    ),
    PluginCategory(
        "styles", "Styles & colours",
        "What happens to <style>, style attributes and colour values.",
    ),
    PluginCategory(
        "attributes", "Attributes & numbers",
        "Rounding, defaults and tidying of ordinary attributes.",
    ),
    PluginCategory(
        "paths", "Shapes & paths",
        "Geometry — the biggest savings on drawing-heavy files.",
    ),
    PluginCategory(
        "structure", "Structure & reuse",
        "Groups, containers and repeated elements.",
    ),
    PluginCategory(
        "ids", "IDs & references",
        "ids, classes, and the links between elements.",
    ),
    PluginCategory(
        "strip", "Strip content",
        "Throws real content away. Only switch these on deliberately.",
    ),
)

/**
 * The 34 plugins SVGO 4.1 runs in `preset-default`; they are the ones that start out enabled.
 * Kept as data rather than read off the library so the catalogue stays common code.
 */
private val PRESET_DEFAULT: Set<String> = setOf(
    "removeDoctype", "removeXMLProcInst", "removeComments", "removeDeprecatedAttrs",
    "removeMetadata", "removeEditorsNSData", "cleanupAttrs", "mergeStyles", "inlineStyles",
    "minifyStyles", "cleanupIds", "removeUselessDefs", "cleanupNumericValues", "convertColors",
    "removeUnknownsAndDefaults", "removeNonInheritableGroupAttrs", "removeUselessStrokeAndFill",
    "cleanupEnableBackground", "removeHiddenElems", "removeEmptyText", "convertShapeToPath",
    "convertEllipseToCircle", "moveElemsAttrsToGroup", "moveGroupAttrsToElems", "collapseGroups",
    "convertPathData", "convertTransform", "removeEmptyAttrs", "removeEmptyContainers",
    "mergePaths", "removeUnusedNS", "sortAttrs", "sortDefsChildren", "removeDesc",
)

private fun plugin(id: String, category: String, label: String, help: String) =
    PluginInfo(id, category, label, help, id in PRESET_DEFAULT)

/** Every plugin the UI offers, **in execution order**. */
public val PLUGINS: List<PluginInfo> = listOf(
    plugin(
        "removeDoctype", "document", "Remove doctype",
        "Drops the <!DOCTYPE> declaration. Browsers ignore it in SVG.",
    ),
    plugin(
        "removeXMLProcInst", "document", "Remove XML instructions",
        "Drops the <?xml …?> processing instruction.",
    ),
    plugin(
        "removeComments", "document", "Remove comments",
        "Drops <!-- … --> comments (legal comments starting with ! are kept).",
    ),
    plugin(
        "removeDeprecatedAttrs", "document", "Remove deprecated attrs",
        "Drops attributes that are deprecated in the SVG specification.",
    ),
    plugin(
        "removeMetadata", "document", "Remove <metadata>",
        "Drops <metadata> blocks — usually editor bookkeeping such as RDF.",
    ),
    plugin(
        "removeXMLNS", "strip", "Remove xmlns",
        "Drops the xmlns attribute. Only for inline SVG inside HTML — a stand-alone file will " +
            "no longer render.",
    ),
    plugin(
        "removeEditorsNSData", "document", "Remove editor data",
        "Drops namespaces and attributes left behind by Inkscape, Illustrator, Sketch…",
    ),
    plugin(
        "cleanupAttrs", "attributes", "Clean up attr whitespace",
        "Collapses newlines and repeated spaces inside attribute values.",
    ),
    plugin(
        "mergeStyles", "styles", "Merge styles",
        "Merges several <style> elements into one.",
    ),
    plugin(
        "inlineStyles", "styles", "Inline styles",
        "Moves CSS rules from <style> onto the matching elements' style attribute.",
    ),
    plugin(
        "minifyStyles", "styles", "Minify styles",
        "Minifies the CSS inside <style> elements and style attributes.",
    ),
    plugin(
        "convertStyleToAttrs", "styles", "Style to attributes",
        "Converts style=\"fill:red\" into presentation attributes like fill=\"red\".",
    ),
    plugin(
        "cleanupIds", "ids", "Clean up IDs",
        "Removes unreferenced ids and shortens the ones that are used.",
    ),
    plugin(
        "removeRasterImages", "strip", "Remove raster images",
        "Drops embedded PNG/JPEG <image> elements.",
    ),
    plugin(
        "removeUselessDefs", "structure", "Remove unused defs",
        "Drops <defs> children that nothing references.",
    ),
    plugin(
        "cleanupNumericValues", "attributes", "Round/rewrite numbers",
        "Rounds numbers to the number precision and strips default px units.",
    ),
    plugin(
        "cleanupListOfValues", "attributes", "Round/rewrite number lists",
        "Applies the same rounding to list-valued attributes such as viewBox.",
    ),
    plugin(
        "convertColors", "styles", "Minify colours",
        "Rewrites colours to their shortest form: rgb(255,0,0) → red → #f00.",
    ),
    plugin(
        "removeUnknownsAndDefaults", "attributes", "Remove unknowns & defaults",
        "Drops non-SVG elements/attributes and attributes set to their default value.",
    ),
    plugin(
        "removeNonInheritableGroupAttrs", "attributes", "Remove unneeded group attrs",
        "Drops non-inheritable presentation attributes from <g> elements.",
    ),
    plugin(
        "removeUselessStrokeAndFill", "attributes", "Remove useless stroke/fill",
        "Drops stroke/fill attributes that have no visible effect.",
    ),
    plugin(
        "removeViewBox", "strip", "Remove viewBox",
        "Drops viewBox when it matches width/height. Off by default in SVGO 4 — removing it " +
            "breaks responsive scaling.",
    ),
    plugin(
        "cleanupEnableBackground", "attributes", "Tidy enable-background",
        "Removes or fixes the enable-background attribute, which no browser supports.",
    ),
    plugin(
        "removeHiddenElems", "structure", "Remove hidden elements",
        "Drops elements that cannot be seen (display:none, zero size, empty paths…).",
    ),
    plugin(
        "removeEmptyText", "structure", "Remove empty text",
        "Drops empty <text>, <tspan> and <tref> elements.",
    ),
    plugin(
        "convertShapeToPath", "paths", "Shapes to paths",
        "Converts <rect>, <line>, <polygon>… into <path> when that is shorter.",
    ),
    plugin(
        "convertEllipseToCircle", "paths", "Ellipse to circle",
        "Converts a non-eccentric <ellipse> into a <circle>.",
    ),
    plugin(
        "moveElemsAttrsToGroup", "structure", "Move attrs to parent group",
        "Hoists attributes shared by all children onto their group.",
    ),
    plugin(
        "moveGroupAttrsToElems", "structure", "Move group attrs to elems",
        "Pushes group transforms down onto the children so groups can collapse.",
    ),
    plugin(
        "collapseGroups", "structure", "Collapse useless groups",
        "Removes <g> wrappers that carry no attributes of their own.",
    ),
    plugin(
        "convertPathData", "paths", "Round/rewrite paths",
        "The big one: rounds path coordinates and picks the shortest command form.",
    ),
    plugin(
        "convertTransform", "paths", "Round/rewrite transforms",
        "Collapses transform lists into a single, shortest-form transform.",
    ),
    plugin(
        "convertOneStopGradients", "structure", "Convert one-stop gradients",
        "Replaces gradients that have a single stop with a plain colour.",
    ),
    plugin(
        "removeEmptyAttrs", "attributes", "Remove empty attrs",
        "Drops attributes whose value is an empty string.",
    ),
    plugin(
        "removeEmptyContainers", "structure", "Remove empty containers",
        "Drops container elements (<g>, <defs>, <symbol>…) with no children.",
    ),
    plugin(
        "mergePaths", "paths", "Merge paths",
        "Merges adjacent paths that share all their styling into one.",
    ),
    plugin(
        "removeUnusedNS", "document", "Remove unused namespaces",
        "Drops xmlns:* declarations that nothing in the document uses.",
    ),
    plugin(
        "removeXlink", "ids", "Replace xlink with SVG 2",
        "Rewrites xlink:href to the modern href attribute.",
    ),
    plugin(
        "reusePaths", "structure", "Reuse duplicate elements",
        "Moves repeated paths into <defs> and references them with <use>.",
    ),
    plugin(
        "prefixIds", "ids", "Prefix IDs and classes",
        "Prefixes every id and class with the file name — useful when several SVGs are inlined " +
            "into the same page.",
    ),
    plugin(
        "sortAttrs", "attributes", "Sort attrs",
        "Sorts attributes into a stable order. No size win on its own, but it compresses better " +
            "with gzip/brotli.",
    ),
    plugin(
        "sortDefsChildren", "structure", "Sort children of <defs>",
        "Orders <defs> children for better compression.",
    ),
    plugin(
        "removeTitle", "document", "Remove <title>",
        "Drops <title>. Off by default in SVGO 4 — it is the SVG's accessible name.",
    ),
    plugin(
        "removeDesc", "document", "Remove <desc>",
        "Drops editor-generated <desc> elements.",
    ),
    plugin(
        "removeDimensions", "strip", "Remove width/height",
        "Removes width/height and keeps viewBox, making the SVG scale to its box.",
    ),
    plugin(
        "removeStyleElement", "styles", "Remove style elements",
        "Drops every <style> element — and with it all CSS styling.",
    ),
    plugin(
        "removeScripts", "strip", "Remove script elements",
        "Drops <script> elements and on… event attributes.",
    ),
    plugin(
        "removeOffCanvasPaths", "paths", "Remove off-canvas paths",
        "Drops paths that lie completely outside the viewBox.",
    ),
)

/** Plugin ids in execution order — this is what drives an SVGO run. */
public val PLUGIN_IDS: List<String> = PLUGINS.map { it.id }

/** [PLUGINS] grouped for display; members keep their execution order inside each group. */
public val PLUGINS_BY_CATEGORY: List<Pair<PluginCategory, List<PluginInfo>>> =
    CATEGORIES.mapNotNull { category ->
        PLUGINS.filter { it.category == category.key }
            .takeIf { it.isNotEmpty() }
            ?.let { category to it }
    }
