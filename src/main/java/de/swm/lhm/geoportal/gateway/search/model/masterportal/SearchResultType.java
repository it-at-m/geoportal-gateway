package de.swm.lhm.geoportal.gateway.search.model.masterportal;

public enum SearchResultType {

    ADDRESS("Adresse"),
    METADATA("Metadaten"),
    GEO_DATA("Geodaten")

    //@formatter:off
    ;
    //@formatter:on

    private final String searchResultType;
    /**
     * constructor with name of the Type
     *
     * 
     */
    SearchResultType(String searchResultType) {
        this.searchResultType = searchResultType;
    }

    /**
     * getter for the name of the SearchResultType
     *
     * @return the name of the SearchResultType
     */
    public String getSearchResultType() {
        return searchResultType;
    }
}