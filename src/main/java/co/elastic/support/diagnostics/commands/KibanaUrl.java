package co.elastic.support.diagnostics.commands;

public class KibanaUrl {
    private boolean paginated;
    private String paginationFieldName;
    private boolean spaceAware;
    private String url;

    // Private constructor
    private KibanaUrl(boolean paginated, String paginationFieldName, boolean spaceAware, String inputUrl) {
        this.paginated = paginated;
        this.paginationFieldName = paginationFieldName;
        this.spaceAware = spaceAware;
        this.url = inputUrl;
    }

    public static KibanaUrl parse(String inputUrl) {
        boolean isPaginated = false;
        String paginationFieldName = null;
        boolean isSpaceAware = false;

        // Single regex pattern to match the end of the string for both cases
        String pattern = "(.*?)(#spaceaware)?(#paginate\\[(.*?)\\])?$";

        // Match the pattern and extract the groups
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = regex.matcher(inputUrl);

        if (matcher.matches()) {
            inputUrl = matcher.group(1).trim(); // The main part of the string without the modifiers

            if (matcher.group(2) != null) { // #spaceaware is present
                isSpaceAware = true;
            }

            if (matcher.group(4) != null) { // #paginate[fieldname] is present
                isPaginated = true;
                paginationFieldName = matcher.group(3); // Extract the field name
            }
        }

        return new KibanaUrl(isPaginated, paginationFieldName, isSpaceAware, inputUrl);
    }

    public boolean isPaginated() {
        return paginated;
    }

    public String getPaginationFieldName() {
        return paginationFieldName;
    }

    public boolean isSpaceAware() {
        return spaceAware;
    }

    public String getUrl() {
        return url;
    }
}
