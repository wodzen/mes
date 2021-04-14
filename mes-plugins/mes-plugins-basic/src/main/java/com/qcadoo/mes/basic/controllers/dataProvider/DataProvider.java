package com.qcadoo.mes.basic.controllers.dataProvider;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.AbstractDTO;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.AdditionalCodeDTO;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.AttribiuteValueDTO;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.CountryDto;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.FactoryDto;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.FaultTypeDto;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.PalletNumberDTO;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.ProductDTO;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.SubassemblyDto;
import com.qcadoo.mes.basic.controllers.dataProvider.dto.WorkstationTypeDto;
import com.qcadoo.mes.basic.controllers.dataProvider.requests.FaultTypeRequest;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.CountriesGridResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.CountriesResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.DataResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.FactoriesResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.FaultTypeResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.ProductsGridResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.SubassembliesResponse;
import com.qcadoo.mes.basic.controllers.dataProvider.responses.WorkstationTypesResponse;
import com.qcadoo.model.api.DictionaryService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

@Service
public class DataProvider {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private DictionaryService dictionaryService;

    public static final int MAX_RESULTS = 20;

    private String prepareProductsQuery() {
        return "SELECT product.id AS id, product.number AS code, product.number AS number, product.unit AS unit, product.name AS name "
                + "FROM basic_product product WHERE product.active = true AND product.number ilike :query ;";
    }

    private String prepareProductsQueryWithLimit(int limit) {
        return "SELECT product.id AS id, product.number AS code, product.number AS number, product.unit AS unit, product.name AS name "
                + "FROM basic_product product WHERE product.active = true AND product.number ilike :query LIMIT " + limit + ";";
    }

    private String prepareAdditionalCodeQuery(final String productnumber) {
        String productNumberCondition = Strings.isNullOrEmpty(productnumber) ? "" : "AND product.number = '" + productnumber
                + "'";

        return "SELECT additionalcode.id AS id, additionalcode.code AS code, product.number AS productnumber "
                + "FROM basic_additionalcode additionalcode "
                + "JOIN basic_product product ON (additionalcode.product_id = product.id " + productNumberCondition + ")"
                + "WHERE additionalcode.code ilike :query;";
    }

    private String prepareAdditionalCodeQueryWithLimit(int limit) {
        return "SELECT additionalcode.id AS id, additionalcode.code AS code, product.number AS productnumber "
                + "FROM basic_additionalcode additionalcode "
                + "JOIN basic_product product ON (additionalcode.product_id = product.id AND (product.number = :productnumber OR COALESCE(:productnumber,'')='' ))"
                + "WHERE additionalcode.code ilike :query LIMIT " + limit + ";";
    }

    private String preparePalletNumbersQuery() {
        return "SELECT palletnumber.id AS id, palletnumber.number AS code, palletnumber.number AS number "
                + "FROM basic_palletnumber palletnumber WHERE palletnumber.active = true AND palletnumber.number ilike :query;";
    }

    private String prepareAttributesQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT av.id as id, av.value as value FROM basic_attributevalue av ");
        builder.append("LEFT JOIN basic_attribute a  ON a.id = av.attribute_id ");
        builder.append("WHERE a.number = :attr AND av.value ilike :query ");
        return builder.toString();

    }

    private String prepareAttributesQueryLimit(int limit) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT av.id as id, av.value as value FROM basic_attributevalue av ");
        builder.append("LEFT JOIN basic_attribute a  ON a.id = av.attribute_id ");
        builder.append("WHERE a.number = :attr AND av.value ilike :query LIMIT " + limit + ";");
        return builder.toString();

    }

    private String preparePalletNumbersQueryWithLimit(int limit) {
        return "SELECT palletnumber.id AS id, palletnumber.number AS code, palletnumber.number AS number "
                + "FROM basic_palletnumber palletnumber WHERE palletnumber.active = true AND palletnumber.number ilike :query LIMIT "
                + limit + ";";
    }

    private int countQueryResults(final String preparedQuery, final String query, final Map<String, Object> paramMap) {
        String countQuery = "SELECT count(*) AS cnt FROM (" + preparedQuery.replace(";", "") + ") sq;";

        String ilikeQuery = buildConditionParameterForIlike(query);
        paramMap.put("query", ilikeQuery);

        return jdbcTemplate.queryForObject(countQuery, paramMap, Integer.class);
    }

    public DataResponse getProductsResponseByQuery(final String query) {
        return getDataResponse(query, prepareProductsQuery(), getProductsByQuery(query), Maps.newHashMap());
    }

    public DataResponse getAdditionalCodesResponseByQuery(final String query, final String productnumber) {
        return getDataResponse(query, prepareAdditionalCodeQuery(productnumber), getAdditionalCodesByQuery(query, productnumber),
                Maps.newHashMap());
    }

    public DataResponse getPalletNumbersResponseByQuery(final String query) {
        return getDataResponse(query, preparePalletNumbersQuery(), getPalletNumbersByQuery(query), Maps.newHashMap());
    }

    public DataResponse getDataResponse(final String query, final String preparedQuery,
            final List<? extends AbstractDTO> entities, final Map<String, Object> paramMap) {
        return getDataResponse(query, preparedQuery, entities, paramMap, true);
    }

    public DataResponse getDataResponse(final String query, final String preparedQuery,
            final List<? extends AbstractDTO> entities, Map<String, Object> paramMap, boolean shouldCheckMaxResults) {
        int numberOfResults = countQueryResults(preparedQuery, query, paramMap);

        if (shouldCheckMaxResults && (numberOfResults > MAX_RESULTS)) {
            return new DataResponse(Lists.newArrayList(), numberOfResults);
        }

        return new DataResponse(entities, numberOfResults);
    }

    public DataResponse getAttributesByQuery(String attr, String query) {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("attr", attr);
        return getDataResponse(query, prepareAttributesQuery(), getAttribiutesByQuery(attr, query), parameters);

    }

    private List<ProductDTO> getAllProducts() {
        String _query = "SELECT product.id, product.number AS code, product.number, product.name, product.unit, product.ean, product.globaltypeofmaterial, product.category "
                + "FROM basic_product product WHERE product.active = true ORDER BY product.number;";
        List<ProductDTO> products = jdbcTemplate.query(_query, new MapSqlParameterSource(Collections.EMPTY_MAP),
                new BeanPropertyRowMapper(ProductDTO.class));
        return products;
    }

    public DataResponse getProductsTypeahead(String query) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder
                .append("SELECT product.id AS id, product.number AS code, product.number AS number, product.unit AS unit, product.name AS name ");
        queryBuilder
                .append("FROM basic_product product WHERE product.active = true AND product.number ilike :query ORDER BY product.number ASC LIMIT 10 ");

        Map<String, Object> parameters = Maps.newHashMap();

        String ilikeQuery = "%" + query + "%";
        parameters.put("query", ilikeQuery);

        List<ProductDTO> products = jdbcTemplate.query(queryBuilder.toString(), parameters, new BeanPropertyRowMapper(
                ProductDTO.class));
        return new DataResponse(products, products.size());
    }

    public ProductsGridResponse getProductsResponse(int limit, int offset, String sort, String order, String search) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT product.id, product.number AS code, product.number, product.name, product.unit, product.ean, product.globaltypeofmaterial, product.category ");
        query.append("FROM basic_product product WHERE product.active = true ");

        StringBuilder queryCount = new StringBuilder();
        queryCount.append("SELECT COUNT(*) ");
        queryCount.append("FROM basic_product product WHERE product.active = true ");

        appendProductsConditions(search, query);
        appendProductsConditions(search, queryCount);

        if (StringUtils.isNotEmpty(sort)) {
            query.append(" ORDER BY " + sort + " " + order);
        }
        query.append(String.format(" LIMIT %d OFFSET %d", limit, offset));

        Integer countRecords = jdbcTemplate.queryForObject(queryCount.toString(),
                new MapSqlParameterSource(Collections.EMPTY_MAP), Long.class).intValue();

        List<ProductDTO> products = jdbcTemplate.query(query.toString(), new MapSqlParameterSource(Collections.EMPTY_MAP),
                new BeanPropertyRowMapper(ProductDTO.class));

        return new ProductsGridResponse(countRecords, products);
    }

    private void appendProductsConditions(String search, StringBuilder query) {
        if (StringUtils.isNotEmpty(search)) {
            query.append(" AND (");
            query.append("UPPER(product.number) LIKE '%").append(search.toUpperCase()).append("%' OR ");
            query.append("UPPER(product.name) LIKE '%").append(search.toUpperCase()).append("%' OR ");
            query.append("UPPER(product.unit) LIKE '%").append(search.toUpperCase()).append("%'");
            query.append(") ");
        }
    }

    public List<AbstractDTO> getProductsByQuery(final String query) {
        String _query = prepareProductsQueryWithLimit(MAX_RESULTS);

        Map<String, Object> parameters = Maps.newHashMap();

        String ilikeQuery = buildConditionParameterForIlike(query);
        parameters.put("query", ilikeQuery);

        SqlParameterSource nParameters = new MapSqlParameterSource(parameters);

        List<AbstractDTO> products = jdbcTemplate.query(_query, nParameters, new BeanPropertyRowMapper(ProductDTO.class));

        return products;
    }

    public List<AdditionalCodeDTO> getAllAdditionalCodes(final String sidx, final String sord) {
        // TODO sort
        String _query = "SELECT additionalcode.id AS id, additionalcode.code AS code, product.number AS productnumber "
                + "FROM basic_additionalcode additionalcode "
                + "JOIN basic_product product ON (additionalcode.product_id = product.id);";

        List<AdditionalCodeDTO> codes = jdbcTemplate.query(_query, new MapSqlParameterSource(Collections.EMPTY_MAP),
                new BeanPropertyRowMapper(AdditionalCodeDTO.class));

        return codes;
    }

    public List<AbstractDTO> getAdditionalCodesByQuery(final String query, final String productnumber) {
        String _query = prepareAdditionalCodeQueryWithLimit(MAX_RESULTS);

        Map<String, Object> parameters = Maps.newHashMap();
        String ilikeQuery = buildConditionParameterForIlike(query);
        parameters.put("query", ilikeQuery);
        parameters.put("productnumber", productnumber);

        SqlParameterSource nParameters = new MapSqlParameterSource(parameters);

        List<AbstractDTO> codes = jdbcTemplate.query(_query, nParameters, new BeanPropertyRowMapper(AdditionalCodeDTO.class));

        return codes;
    }

    public List<PalletNumberDTO> getAllPalletNumbers(final String sidx, final String sord) {
        String _query = "SELECT palletnumber.id AS id, palletnumber.number AS code, palletnumber.number AS number "
                + "FROM basic_palletnumber palletnumber WHERE palletnumber.active = true;";

        List<PalletNumberDTO> pallets = jdbcTemplate.query(_query, new MapSqlParameterSource(Collections.EMPTY_MAP),
                new BeanPropertyRowMapper(PalletNumberDTO.class));

        return pallets;
    }

    public List<AbstractDTO> getPalletNumbersByQuery(final String query) {
        String _query = preparePalletNumbersQueryWithLimit(MAX_RESULTS);

        Map<String, Object> parameters = Maps.newHashMap();

        String ilikeQuery = buildConditionParameterForIlike(query);
        parameters.put("query", ilikeQuery);

        SqlParameterSource nParameters = new MapSqlParameterSource(parameters);

        List<AbstractDTO> pallets = jdbcTemplate.query(_query, nParameters, new BeanPropertyRowMapper(PalletNumberDTO.class));

        return pallets;
    }

    public List<AbstractDTO> getAttribiutesByQuery(final String attr, final String query) {
        String _query = prepareAttributesQueryLimit(MAX_RESULTS);

        Map<String, Object> parameters = Maps.newHashMap();

        String ilikeQuery = buildConditionParameterForIlike(query);
        parameters.put("query", ilikeQuery);
        parameters.put("attr", attr);

        SqlParameterSource nParameters = new MapSqlParameterSource(parameters);

        List<AbstractDTO> attrs = jdbcTemplate.query(_query, nParameters, new BeanPropertyRowMapper(AttribiuteValueDTO.class));

        return attrs;
    }

    public List<Map<String, String>> getUnits() {
        return dictionaryService.getKeys("units").stream().map(unit -> {
            Map<String, String> type = Maps.newHashMap();

            type.put("value", unit);
            type.put("key", unit);

            return type;
        }).collect(Collectors.toList());
    }

    public List<Map<String, String>> getTypeOfPallets() {
        return dictionaryService.getKeys("typeOfPallet").stream().map(unit -> {
            Map<String, String> type = Maps.newHashMap();

            type.put("value", unit);
            type.put("key", unit);

            return type;
        }).collect(Collectors.toList());
    }

    private String buildConditionParameterForIlike(String query) {
        String ilikeQuery = "%" + query + "%";
        ilikeQuery = ilikeQuery.replace("*", "%");
        ilikeQuery = ilikeQuery.replace("%%", "%");
        return ilikeQuery;
    }

    public WorkstationTypesResponse getWorkstationTypes() {
        StringBuilder query = new StringBuilder();
        query.append("Select w.id as id, w.number as number, w.name as name From basic_workstationtype w WHERE ");
        query.append(" w.active = true ORDER BY w.number ");
        List<WorkstationTypeDto> workstationTypes = jdbcTemplate.query(query.toString(), Maps.newHashMap(),
                new BeanPropertyRowMapper(WorkstationTypeDto.class));
        return new WorkstationTypesResponse(workstationTypes);
    }

    public CountriesResponse getCountries(String query) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("Select c.id as id, c.code as code, c.country as country From basic_country c WHERE ");
        queryBuilder.append(" 1=1 ");
        Map<String, Object> parameters = Maps.newHashMap();

        queryBuilder.append(" AND c.country ilike :query ORDER BY country ASC LIMIT 10 ");

        String ilikeQuery = "%" + query + "%";
        parameters.put("query", ilikeQuery);
        List<CountryDto> countries = jdbcTemplate.query(queryBuilder.toString(), parameters, new BeanPropertyRowMapper(
                CountryDto.class));
        CountriesResponse response = new CountriesResponse();
        response.setCountries(countries);
        return response;
    }

    public CountriesGridResponse getCountriesByPage(int limit, int offset, String sort, String order, String search) {
        StringBuilder query = new StringBuilder();
        query.append("Select c.id as id, c.code as code, c.country as country From basic_country c WHERE ");
        query.append(" 1 = 1 ");

        StringBuilder queryCount = new StringBuilder();
        queryCount.append("SELECT COUNT(*) ");
        queryCount.append("From basic_country c WHERE 1 = 1 ");
        Map<String, Object> parameters = Maps.newHashMap();

        appendCountriesConditions(search, query, parameters);
        appendCountriesConditions(search, queryCount, parameters);

        if (StringUtils.isNotEmpty(sort)) {
            query.append(" ORDER BY " + sort + " " + order);
        }
        query.append(String.format(" LIMIT %d OFFSET %d", limit, offset));

        Integer countRecords = jdbcTemplate.queryForObject(queryCount.toString(), parameters, Long.class).intValue();

        List<CountryDto> countries = jdbcTemplate
                .query(query.toString(), parameters, new BeanPropertyRowMapper(CountryDto.class));

        return new CountriesGridResponse(countRecords, countries);
    }

    private void appendCountriesConditions(String search, StringBuilder query, Map<String, Object> parameters) {
        if (StringUtils.isNotEmpty(search)) {
            query.append(" AND (");
            query.append("UPPER(c.code) LIKE '%").append(search.toUpperCase()).append("%' OR ");
            query.append("UPPER(c.country) LIKE '%").append(search.toUpperCase()).append("%' ");
            query.append(") ");
        }
    }

    public FactoriesResponse getFactories() {
        StringBuilder query = new StringBuilder();
        query.append("Select w.id as id, w.number as number, w.name as name From basic_factory w WHERE ");
        query.append(" w.active = true ORDER BY w.number ");
        List<FactoryDto> factories = jdbcTemplate.query(query.toString(), Maps.newHashMap(), new BeanPropertyRowMapper(
                FactoryDto.class));
        return new FactoriesResponse(factories);
    }

    public SubassembliesResponse getSubassemblies(Long workstationId) {
        Map<String, Object> parameters = Maps.newHashMap();
        parameters.put("workstationId", workstationId);
        StringBuilder query = new StringBuilder();
        query.append("Select w.id as id, w.number as number, w.name as name From basic_subassembly w WHERE ");
        query.append(" w.active = true AND workstation_id = :workstationId ORDER BY w.number ");
        List<SubassemblyDto> subassemblies = jdbcTemplate.query(query.toString(), parameters, new BeanPropertyRowMapper(
                SubassemblyDto.class));
        return new SubassembliesResponse(subassemblies);
    }

    public FaultTypeResponse getFaultTypes(FaultTypeRequest faultTypeRequest) {

        if(Objects.nonNull(faultTypeRequest.getSubassemblyId())) {
            Map<String, Object> parameters = Maps.newHashMap();

            parameters.put("subassemblyId", faultTypeRequest.getSubassemblyId());

            StringBuilder query = new StringBuilder();
            query.append("select s.id as id, s.name as name, s.name as number from jointable_faulttype_subassembly jfs ");
            query.append("left join basic_faulttype s ON s.id = jfs.faulttype_id ");
            query.append("where jfs.subassembly_id = :subassemblyId ");

            List<FaultTypeDto> types = jdbcTemplate.query(query.toString(), parameters, new BeanPropertyRowMapper(
                    FaultTypeDto.class));

            return new FaultTypeResponse(types);
        } else if(Objects.nonNull(faultTypeRequest.getWorkstationId())) {

            Map<String, Object> parameters = Maps.newHashMap();

            parameters.put("workstationId", faultTypeRequest.getWorkstationId());

            StringBuilder query = new StringBuilder();
            query.append("select s.id as id, s.name as name, s.name as number ");
            query.append("from jointable_faulttype_workstation jfw ");
            query.append("left join basic_faulttype s ON s.id = jfw.faulttype_id ");
            query.append("where jfw.workstation_id = :workstationId ");

            List<FaultTypeDto> types = jdbcTemplate.query(query.toString(), parameters, new BeanPropertyRowMapper(
                    FaultTypeDto.class));

            return new FaultTypeResponse(types);
        } else {
            return new FaultTypeResponse(Lists.newArrayList());
        }


    }
}
