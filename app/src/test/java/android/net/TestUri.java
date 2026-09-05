package android.net;

import java.util.Collections;
import java.util.List;

public class TestUri extends Uri {
    private final String uriString;

    public TestUri(String uriString) {
        super();
        this.uriString = uriString;
    }

    public TestUri() {
        this("content://scanflow/test_uri");
    }

    @Override
    public boolean isHierarchical() { return true; }

    @Override
    public boolean isRelative() { return false; }

    @Override
    public String toString() { return uriString; }

    @Override
    public String getScheme() { return "content"; }

    @Override
    public String getEncodedSchemeSpecificPart() { return ""; }

    @Override
    public String getEncodedAuthority() { return ""; }

    @Override
    public String getEncodedPath() { return ""; }

    @Override
    public String getEncodedQuery() { return null; }

    @Override
    public String getEncodedFragment() { return null; }

    @Override
    public String getPath() { return ""; }

    @Override
    public String getQuery() { return null; }

    @Override
    public String getFragment() { return null; }

    @Override
    public String getAuthority() { return ""; }

    @Override
    public String getSchemeSpecificPart() { return ""; }

    @Override
    public List<String> getPathSegments() { return Collections.emptyList(); }

    @Override
    public String getLastPathSegment() { return null; }

    @Override
    public Builder buildUpon() { return null; }

    @Override
    public String getHost() { return "scanflow"; }

    @Override
    public int getPort() { return -1; }

    @Override
    public String getUserInfo() { return null; }

    @Override
    public String getEncodedUserInfo() { return null; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(android.os.Parcel dest, int flags) {}

    @Override
    public int compareTo(Uri other) { return 0; }
}
