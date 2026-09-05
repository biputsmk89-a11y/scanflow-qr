package android.net

/**
 * Lightweight mock implementation of [Uri] for JVM local unit testing.
 * Avoids stub exception "Method parse in android.net.Uri not mocked".
 */
class TestUri(private val uriString: String = "content://scanflow/test_uri") : Uri() {
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun toString(): String = uriString
    override fun getScheme(): String = "content"
    override fun getEncodedSchemeSpecificPart(): String = ""
    override fun getEncodedAuthority(): String = ""
    override fun getEncodedPath(): String = ""
    override fun getEncodedQuery(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getPath(): String = ""
    override fun getQuery(): String? = null
    override fun getFragment(): String? = null
    override fun getAuthority(): String = ""
    override fun getSchemeSpecificPart(): String = ""
    override fun getPathSegments(): List<String> = emptyList()
    override fun getLastPathSegment(): String? = null
    override fun getUserInfo(): String? = null
    override fun getHost(): String = "scanflow"
    override fun getPort(): Int = -1
    override fun buildUpon(): Builder? = null
    override fun compareTo(other: Uri?): Int = 0
}
