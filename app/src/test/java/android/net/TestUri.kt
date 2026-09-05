package android.net

import android.os.Parcel

/**
 * Lightweight mock implementation of [Uri] for JVM local unit testing.
 * Implements all abstract methods of [Uri] and [Parcelable].
 */
class TestUri(private val uriString: String = "content://scanflow/test_uri") : Uri() {
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun toString(): String = uriString

    override fun getScheme(): String = "content"
    override fun getSchemeSpecificPart(): String = ""
    override fun getEncodedSchemeSpecificPart(): String = ""

    override fun getAuthority(): String = "scanflow"
    override fun getEncodedAuthority(): String = "scanflow"

    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null

    override fun getHost(): String = "scanflow"
    override fun getPort(): Int = -1

    override fun getPath(): String = ""
    override fun getEncodedPath(): String = ""

    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null

    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null

    override fun getPathSegments(): List<String> = emptyList()
    override fun getLastPathSegment(): String? = null

    override fun buildUpon(): Builder? = null
    override fun compareTo(other: Uri?): Int = 0

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
}
