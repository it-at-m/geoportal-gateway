package de.swm.lhm.geoportal.gateway.search.model.mapper;

import de.swm.lhm.geoportal.gateway.search.model.masterportal.SearchResultTo;

public interface ResultMapper<T>{
  SearchResultTo map(T document);
}
