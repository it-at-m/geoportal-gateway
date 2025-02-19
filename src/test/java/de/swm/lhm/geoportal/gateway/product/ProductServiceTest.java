package de.swm.lhm.geoportal.gateway.product;

import de.swm.lhm.geoportal.gateway.base_classes.BaseIntegrationTest;
import de.swm.lhm.geoportal.gateway.product.model.License;
import de.swm.lhm.geoportal.gateway.product.model.Product;
import de.swm.lhm.geoportal.gateway.util.DataBufferUtils;
import de.swm.lhm.geoportal.gateway.util.ExtendedURIBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;


@Import({
        ProductService.class
})
@ExtendWith({OutputCaptureExtension.class})
class ProductServiceTest extends BaseIntegrationTest {

    @Value("${geoportal.gateway.product.endpoint}")
    private String productsEndpoint;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void setUp() throws IOException {

        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/generic/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/geoservice/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/metadata/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/portal/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/resource/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/sensor/setup.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/unit/setup.sql"));

        runSql("""
                CREATE TABLE IF NOT EXISTS t_product
                (
                    id integer NOT NULL,
                    name character varying(128) NOT NULL,
                    title character varying(255) NOT NULL,
                    description character varying(2000),
                    license character varying(8) NOT NULL DEFAULT 'CC_BY_40'::character varying,
                    header_image bytea,
                    header_image_file_name character varying(255),
                    validity_period_start timestamp with time zone,
                    validity_period_end timestamp with time zone,
                    unit_id integer,
                    metadata_id integer,
                    updated_timestamp timestamp without time zone NOT NULL DEFAULT now(),
                    stage character varying(13) NOT NULL DEFAULT 'UNSTAGED'::character varying,
                    in_transport boolean NOT NULL DEFAULT false,
                    in_transport_time_start timestamp without time zone,
                    access_level character varying(11) NOT NULL DEFAULT 'PUBLIC'::character varying(11),
                    auth_level_high boolean DEFAULT false,
                    promote_request_time timestamp without time zone,
                    demote_request_time timestamp without time zone,
                    role_name character varying(256) NOT NULL
                );
                
                CREATE TABLE IF NOT EXISTS t_product_layer
                (
                    product_id integer NOT NULL,
                    layer_id character varying(255) NOT NULL,
                    layer_type character varying(255) NOT NULL
                );
                """);
                runSql("""
                INSERT INTO t_product
                (id, name, title, description, license, header_image, header_image_file_name, validity_period_start, validity_period_end, unit_id, metadata_id, stage, access_level, auth_level_high, role_name)
                VALUES
                (1, '""" + PUBLIC_PRODUCT + "', 'title:" + PUBLIC_PRODUCT + "', 'description:" + PUBLIC_PRODUCT + "', 'CC_BY_40', null, null, DATEADD(DAY, -1, CURRENT_DATE) + TIME '10:50:00', DATEADD(DAY, +1, CURRENT_DATE) + TIME '10:50:00', 1, 1, 'CONFIGURATION', 'PUBLIC', false, '" + PUBLIC_PRODUCT + "')," +
                "(2, '" + PUBLIC_PRODUCT + "2', 'title:" + PUBLIC_PRODUCT + "2', 'description:" + PUBLIC_PRODUCT + "2', 'DL_20', null, null, null, null, 1, 1, 'CONFIGURATION', 'PUBLIC', false, '" + PUBLIC_PRODUCT + "')," +
                "(3, '" + PUBLIC_PRODUCT + "3', 'title:" + PUBLIC_PRODUCT + "3', 'description:" + PUBLIC_PRODUCT + "3', 'DL_20', null, null, DATEADD(DAY, -1, CURRENT_DATE) + TIME '10:50:00', NOW(), 1, 1, 'CONFIGURATION', 'PUBLIC', false, '" + PUBLIC_PRODUCT + "')," +
                "(4, '" + PUBLIC_PRODUCT + "4', 'title:" + PUBLIC_PRODUCT + "4', 'description:" + PUBLIC_PRODUCT + "4', 'DL_20', null, null, null, null, 1, 1, 'QS', 'PUBLIC', false, '" + PUBLIC_PRODUCT + "')," +

                "(5, '" + PROTECTED_PRODUCT + "', 'title:" + PROTECTED_PRODUCT + "', 'description:" + PROTECTED_PRODUCT + "', 'DL_20', null, null, null, null, 1, 1, 'CONFIGURATION', 'PROTECTED', false, '" + PROTECTED_PRODUCT + "')," +
                "(6, '" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "', 'title:" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "', 'description:" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "', 'DL_20', null, null, null, null, 1, 1, 'CONFIGURATION', 'PROTECTED', true, '" + PROTECTED_AUTH_LEVEL_HIGH_PRODUCT + "')," +
                "(7, '" + RESTRICTED_PRODUCT + "', 'title:" + RESTRICTED_PRODUCT + "', 'description:" + RESTRICTED_PRODUCT + "', 'DL_20', null, null, null, null, 1, 1, 'CONFIGURATION', 'RESTRICTED', false, '" + RESTRICTED_PRODUCT + "');"

                );
    }

    @AfterEach
    void tearDown() throws IOException {

        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/generic/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/geoservice/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/metadata/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/portal/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/resource/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/sensor/teardown.sql"));
        runSql(loadFileContent("de/swm/lhm/geoportal/gateway/unit/teardown.sql"));

        runSql("""
                DROP TABLE IF EXISTS t_product;
                DROP TABLE IF EXISTS t_product_layer;
                """.split(";"));
    }

    @Test
    void getAllPublicAndProtectedProducts() {
        List<Product> result = productService.getAllProducts().collectList().block();

        assertThat(result, hasSize(3));

        List<String> names = result.stream().map(Product::getName).toList();
        assertThat(names, containsInAnyOrder(List.of(PUBLIC_PRODUCT, PUBLIC_PRODUCT + "2", PROTECTED_PRODUCT).toArray()));

        for (Product product : result) {
            if (product.getId().equals(1)){
                assertThat(product.getLicense(), is( License.CC_BY_40));
            } else {
                assertThat(product.getLicense(), is( License.DL_20));
            }

        }
    }

    @Test
    void getAllPublicAndProtectedAuthLevelHighProducts() {
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(PROTECTED_AUTH_LEVEL_HIGH_PRODUCT), true)
                )
                .get()
                .uri(productsEndpoint)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class).value(result -> {
                    assertThat(result, hasSize(4));
                    List<String> names = result.stream().map(Product::getName).toList();
                    assertThat(names, containsInAnyOrder(List.of(PUBLIC_PRODUCT, PUBLIC_PRODUCT + "2", PROTECTED_PRODUCT, PROTECTED_AUTH_LEVEL_HIGH_PRODUCT).toArray()));
                });
    }

    @Test
    void getRestrictedProducts() {
        webTestClient.mutateWith(
                        keyCloakConfigureGrantedProducts(List.of(RESTRICTED_PRODUCT))
                )
                .get()
                .uri(productsEndpoint)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class).value(result -> {
                    assertThat(result, hasSize(4));
                    List<String> names = result.stream().map(Product::getName).toList();
                    assertThat(names, containsInAnyOrder(List.of(PUBLIC_PRODUCT, PUBLIC_PRODUCT + "2", PROTECTED_PRODUCT, RESTRICTED_PRODUCT).toArray()));
                });
    }


    @Test
    void getProductImageByProductName() throws URISyntaxException {

        runSql("DELETE FROM t_product");
        runSql("""
                INSERT INTO t_product (
                id, name, title, description, license,
                header_image,
                header_image_file_name, validity_period_start, validity_period_end, unit_id, metadata_id, updated_timestamp, stage, in_transport, in_transport_time_start, access_level, auth_level_high, promote_request_time, demote_request_time, role_name)
                VALUES
                (761392, 'test_product', 'ta_produkt18120358_', 'Beschreibung: automatisierter Test Produkt bearbeiten', 'NB_LHM',
                '\\xffd8ffe000104a46494600010100000100010000ffdb0084000a0708120f1112110f120f0f0f0f0f0f0f0f0f111211110f0f0f18141919181418181c212e251c1e2c1f18182638262b2f313736351a253b4048373f2e343531010c0c0c100f101a12121a3427232735313b353631343134343734313f343534363435313134343134343434313434313434343f34343436343431313437343135ffc000110800b1011c03012200021101031101ffc4001c0001000203010101000000000000000000000104020305070608ffc4003a10000202010204040405020503050000000102000311041205132131062241510732617114428191a12352156282d1e172c1f0162433a2b2ffc4001a010101000301010000000000000000000000010203040506ffc400261101010002010401040203000000000000000102110304122131410551618171d14291a1ffda000c03010002110311003f00fac4a66e144b15d737ad7336b50e449e44bfcb8e5c0a1c88e44bfcb8e5c0a1c88e44bfcb93b2073f911c89d0d92797039fc88e44e872e397039fc88e44e872a395039fc88e44e872a3640e7f224f225fe5472a050e447225fe5472a050e447225fe549e5c0a1c88e44be2b8d90287223913a012397039fc891c89d0e5c6c8143911c897f971cb81439123913a1cb91b2050e447225fe5472a0735a89a8d53a8e92bb57d606fa84dea269aa58580c4624c42231189310a8c49c4491288c49c498904623126210c4624c408c4624ca3acd7a232d15d9a63adb3672b4d6dcb5bbab360bedeac42a876c01d76e077cc2aee2313cfbc67f116ce15a86d1d7a7aefbeb446b3537865a99dd43792b539da338c96ce723d3277f813e22af1136d5ae5d3e9ef0a0d0d4ee5e7139055118b79c743d339cf6e926d7b5f7588c4e1f03d4ea8ee6d58b2aabbd5f8aab4da6bf1d70329730623a7744ef9fa4e951c46a72143aab9ec8cca189f6041c37e84cbb4b34b7b63126211188db26214c48c498811b631273102311893102311b64c40c711893108d4e25671d65a7955fbc2b2aa58595ea961604c444211110111329548889024c812610889c1f10f18d4d0d453c3b4e357acd43d98563b6baa9aca731d8e47f7aa839c024f72310b23e7fc63c6f5fa8d459c2b845171d456a8755a8055394ae819551c90132a4798907b81d7acabe13d26a3855ba9d5f15d3bddc5752c89a3dd650fcc1b58315652796a06d0c4e005da07b4a7f1bb89eb2aba9a158d3a3b69e6ff4ced375ca4ab0723a90a36e076f30f5ed57c134f234c2dbd98b58320bb922bafd1467e507b9ff0089c9d5f5178b8fba4ddbe24fcb771e1bba7d16af82ff00895c96f10e5ea6e03095a28ae8a5739da08f33019fcc4fd86713b83429c3d4d3a0d3a7e25972ef5575ab2647c887031ee58fbfd80e770be2a8b62edf366cd8cde8bb73e5fd48fe333b3c0cd8fc42f72e4534d14d653a62cd45a05acc4fd15904e7e870e4cb1bc9cb95b6efc7dbf4bcb64cb58fa72c785f5578365b6b2bb75daa8ac7ec5acea4fd40139fabf0fea74e37156603b9d8aa7efb9329fa301f713d25b5f4ab0adacad5dbe546750cdf604e4cb21819d17872df763c965ff89dd3d591f07e1ee3e5d85377cfd91bb6ec77041ec47b7ed3e9e713c69c15796757a71b2ea48b1b6f40e01ef8f7c67afb1fb4e9e82fe6d55d83f3a2b7ebd8ff0020ceac2dd79f6d59492f858888950888842222022221491110844440d6f2abf7965e567ef0acaa961657aa58581311108444404ca633295488891093224c07fb81fb9c4f25f1afc4fd5d5aab74da0e5d15696d6a4da6b5b2db5d0e1f3bb202ee07a019e99cf5e9e8be21e2a9a2a92eb89150d4d08ec3f2ab36371fa03827e80cf1ee3be14d7a715b6dab4ada9adf5adaaa9827334f6235bbc2b9ec075c107ebe9d64acf17da78aed4e33e1f4d7df4f2b53428b2b3d400dbd51cae7ba3001b1f41ed99e5c9a963b435d9008013048fb779ec1f133c434d3a71c3cab35fac1515000e5a522c1bb71fa856000fe27cbd1c3a935e3969e65c1f28cf5138fa9ea71e1b378ef6ddc5c57397ca870ab0ef76058ec6b19727a7429fb9c67f99ea5c2b5b5ed0ea40fc4257613eeca8b59ff00f03f79e62da34a76a57e5da43a75eedebdfd7fde777866a7081376d4ddbaa739c53611d51bd94fa1fb7b4cba7ea71e5dc918f2f15c5cbe29a2d53daee6cad76bb9d40b09de4924f97dc63a0f618f49ea3e1bd45834948b8b1b056a18b67711f9739f5db8ccf8d7e22eae03a235898c6f09bd47a119f4fa8e93ada3e2ec7e7207dc88e3cf8f1b779cfdfb5cfbb293c3ea78b6a0369ae07a834b8fbe54ca3c213954d74b7ce8afe8718c8cf5fb9fe66ad3ea16e5de7cd4230666ecb6ba9cad699f9ba8193dba63edc1e0de283a9e29669171c94d35d613fdd72da8b91f63cc1fa89d332c6c9aad365f97d8c408993122220222202220c088888088881ade557ef2d3caafde159572c2caf5cb0b0262221088885226512a111120988881c2f16e8b4fa9a2ba357cce55daaa2a0108525c93b4127b2e7be3acf82a3c79668b88fe0ef435682871a6feaee6d4d6a0616d67272c3b1f5f291dfb9faaf89943dda5d3d15b15b351c4b474a30ce55d8b60f4f63d7f49e25553a9e23aaae877b2ed4d8cba7536b3315c74c3139381d7f6325ace47b2fc48d2e9b51a54b5b3f89d395b34eea090f5b3287438072bb4eecfa6dce719cf1f483fa63ec259d3b68b8450741ade20ba9d432b2569b5b6e97756576b30276af987cc474eb803334d4c36823b1008fb4f1feabfe3fb75f4beaa9f14a03267d8fa771f51386ba9ba939562c3b16c6e0cbfdae9f987d44fa6b06e523dc4e2357838f69c7d372dc1d59e1329aabba6e375da812f52a17b0c1b6b53fe53f3a7da76743acd1a631569dd87ab8d4d9ffd18edfe27cd269c13d403f713a3a7a02e0627567d6fcdc65bf9699d37db2b23e9afe2af7e3a9c28c2f40a1476c2a8e8a2703e18709b1b59a9d7b8fe9f32fd3d049f994365f03d8129d7ead2f53e5527d949fe275fe1f58ad432a8c0a569563fdd75882e761f4c3d63ee866cfa772e7cbc99e5979f4d5d4e38e38c98beb60c44f5dc6444184222202222044444044440d4f2b3f7965e566ef0acea9bc4d3509bd44044cb118846313288088938811126202222073f8b696bb8e9d6e4e601aa4b10e581aedad1dd5c11f55c7faa7cf719f12f07e1f7d163515b5bbf5358d4514545b4ee876b827a1ce588e99f59f41c71ac5a43d2963badd4b85419672aeadb3e81b1b0b1e8a18b1e80cf1ef15d6dc3b44fa2d73aea75faebc6b82ae5aad02966dcc8c4756b0e4103a617ec4caca477bc35c5383eb35babd2b50ef6713bf57b35d62a0c8b0b909583d6be87a1ee4f7c7403bbc67c36bc2e8ad92c3650a2ba59ad2a2c1663008f707dbb8fafa7907857826af5ba841a35656aec476d46309a7208218b76c8c640ee71d27e9cb6b4b576588962e436d75575241e8707ebeb3473f0ce6c7b6fe9b70cee196e3cbab1bbb4a5acd1904951913b3e29d20e16e1ca91a1b5d512dcefe4da72796e3b81d0e1ba8f7c1ef8e9f508e819195d58643020823e867ce7371727065e67f4f431e4c729b8e470bd1971a866caf212b75181862d60520fb633997d2922743445797a9edd56853fadabfed33545dbdc0c7bf69873e7e31b27b8ca65adb85c7f5674fa6b19725dd4d68075259fca303f59f55f0daa6ff0f1758c8d66a6d6b19510a0aca2251b08fee1c9c9fa933e61787dbc5350aba3d4514ae8dd6fbee67566a76b1c10a33d72a7be0749e9cbadaee653490f5bd297d6ca3c8eae4b6e5f73d549ff00a87bcf73e9bc378f8b7679ae1ea72eeba9f0ce24c4f49ca889323101118880912631088893220222206a7959fbcb2f2b3f7856cae5959a2b12c2880893b63102224e23108888c49c408889388112506481138be30d6be97437ea296d9669f956afb305b50b21fa32e57fd50aeaf1ab74fa5d3db66aae14d2c72ceecdd095002201d7b2f40bd7393d4933ccb595e838a014dd6d7a8b1131a7d654e05fcace4061ee33d5587739e9996fe366a45bc3b42ea7296ea058becca69254fecdfccd5f0dbe1fd6fa21adbcb1d46a558e980255684c9018e0f98b633d7a608f591959f2eff860e8f478e1da6602ca916fb14e79966ec03633630c7b76ec303b62792f88f8e6b28e2f7ea398e9a8d3ea6c5afa9c2d2ac76263b142b8e9eb939ef3b1c6e8d6f0fe355b3594d56da2bc7f5108fc38c2957cf6ced381dcfa7a49f1ef87aed5ea86a7495f305b5a8b4064055d46d0c724742a17f63149e2f97d2e8fe27e8b88b26935da322ad4f2eb72ccb6d42d2c3bae010a0e086ee3f49f775786b47a6a5eaa74b4256f9dca17258e3192c7cdfccf0ae0de06d7bdf4f32be457cc52f69b2b3cb40724e14939e9d063be3b779e91f143c57aad22e99346cb5adece2eb76a3ba9caed501b20646feb8f4e98c1935bf717f87774baad2ea5192b527976b54e958a812d4d832bb48f97207e8653e3fc2f41adacd65afd3b2963bb4f72545f1d363062548390718cfec448e0c95e9ab274c6bd30b72f712a1dae7c77259b39ef9399628715920354039085942e4ab5a50e0f71e5ebd0cdb3a7c2c97b63cbe6eb3970cf532f1fc7faf968f0d781f4fa3e17a8a2dbbcdada59b59aca8ed02ac12150b0f902e7b8ebb9bb761c8e2de24ab800a34c3997a305da728eff00874aeba9483d000c2b0d81ea7f6eed3aed26ed3e9eeb95b55525e6aa058c16ea5d595b72e76b82b92037518ce0759e75e2be30abaebf54a2b77d3af2b4f5b00ca8cae54be3f6fda6171edb63d0e3cfbf19959ee3d8b87eb6bd4d4b753bf631c61eb7a981c03d55802320820e3a832c4f1df0ef8bb8aaea691aeb77520b33688a5356a6ea98100a2ed0ce54f982e7736dc0ce44f60aac57557421d1d55d1875564232187d0832465632888950888808c44408c48226520c048932211ade557ef2d3cacdde1562a137a89aaa12c28856388c4cf118846189189b31231030c462678918818e23132c488112b712d057aaa6ca2f5dd55ca51d738383ea0fa10707f496b1103c67e26f0dd457a6d1e9cee7ab878b6aad80e9669c8508e40fccaa815bf7e80cb9f06bc626b71c3750e396fb9b48cdf91fe63567d8f523eb91ea27a96bf4156aab35de896a120ed61f2b0ecca7bab0f423a89f9da808cf6d9601a63a7bd455a84052c4b3792a0e3cac46def8cfa93258ca576be332d9fe2f633a95ada9a050de8e8106e20fd1f78fd259f83dc3d759ab75badd48af4f52da94a5ae94d8db82e1f07b751d077fb743d71e27d06a82e978ca2b3a76728fb3240f3291d5723d8cfa4e05aae13c3d5db401539db4b30e6396033b7ab67a753da34773ade2dd468b48811b554e83517865a19d5ac40d8f9990765fa9207dfb4f14aafbecbf5556aee1a816aec1a9197a59eab0356f5b103cbf363006439f79c1e3755a9a8b79eceeed63b9b5ba9b81627999f5cf79dcd2f1805155d436d00038ebd0442fe1e8fa5e29a74743a83bc56be4a1577b58fe848edb4633d7d7135a712a5f76ab524534b5bb39558fea59711d2a451dba632debd4fa923cf35dacb2c20509b430ea460648f7265ed5700bc70f5bb783626a43baeec2d68c985ebef91dff00cd364e4b3d39393a5e3cecee9f3bd7defe553c41abb97595eaf4e8ea6bd9b320b61973953ee3071f59dda355a1aafbf5e69d46b352337d7a6da9550802aef60ccac59d71bf3b4600247cb99f29a4d46a37ed18703cccc0ee403ea65f3c69aa7ad95706abe9b085192db5d588fae718c4c2deeb6d74e1876498cf51eac3806838ee8b4faba8d9a7bcd86da757845d6214b30d5bb0f9f041018e48c29ebd73f60b5aa792b0a113c8a17a2803a003e8318fd270785d1a6ad069b408e2aa52c35de413555cfb05aca09e8e474200ce36807199de4408a157e550147a9c01ef248b6988c4ca44a88c4624c40c626510318996246206388c4ca41103538955c75972c955fbc0b554de257a8cdea6159448881322246204c88c46d842449db236c048cc9db1b6046678cf8c3812e86cd69b817d2eaaf5d4d7b079ebdede720ff959bb7b01ef3d9f6ce5f8878157c4286a6c66427aa58a01746f7c1e8c0f62a7b8857843eb348f6d09b989a456a9a838dae00e8adfefe8733a1622d56ee3636dbf08959ea81c0ee31dbfe663e24f873acd19665437d3df9942b381ff00520cbafecc07bce453c4568415ee2cca3a6e3d370fcb9c74c7b7a492967d97b8d6a6945d970e6363722648607b021bf2ff00e779f3f53823a7acb1c4b6ddb6c70f4bb2e1778f25807b1fd7bcd35e8dd0116ff4c7e5de08c93ff68ab26a36d1ad23a7461d8a91d25dd4f16b1ab441bdb4c8e0db592c6b2d8f206fa74380673d69b1bfa7a656bdac2bb8569cc6dfd428500123bfdccf40f0778138930ffdcd8fa1d293b9abc56d758718042f50bdcf56ed8ec621647c36af893b80aa02a9eaa8a36ae3de7defc32f092ea776b35b5ad9a7da534f5d8a196e7cf9ac2a7ba8c607b927dbafd41f86ba36d40bed7d4de802e28b1d4a1c7a330018afae33ebedd27d8a541142a00aaa02aaa8015540c0000ec2346db170000300000003a003d849ccc3698da6562cf3199af063ac0d998ccd7d632606ccc6661991ba06c89af749dd0338986e8dd031b2576ef37b9959cf58166a965655a8cb0a6159c4c73273032898ee91ba067811d261ba41781b32241226a2f35b3426dbcb8989b44aae4cd0fba0dae59ab441962140f52401286a3c414276b158fb2e4ca9acd3f314ab0c833e6f57e1c273b2c75fa101841b76751e2827e52107f33ca3c75c5d6cd63b8c5ac6ba95d8e06d619f2741d7a1ce4fbe3d27d36a7c35a9fcb6237df72cf9ad7782756599d36b331248665c75fac55c7f2b5e0cf18ad3bebd63e0135f25f6e42f57c8623b0f30c1f612cfc49e314dda7aab501ac366e56c93b100f311f52768fde7ce1f086bd0e4520e3d994cc9bc37a82179d55d90300a233803dba193ce97c6f6afe11f10ff0086de5d939953802c03e7180769439001c9f5f49eb7c2fc55a7d4a074674cf428fe5643ec467f913c97ff0049ea4f55aedc7a66b6cceb70bf0eead33e4b0e40182bb7b7dcc4d9969eb3571a4f4b0fef99d1a78c2e327ccbee089e79c3b865ea3cda70cdfdcf60c0ff00489d7a785ea5ff00f92c4451d950640fd8099307dcd1c46a7e81b07d8cb6184f95d0f0d15e0b16761ea7a01fa4ec2399176e9748c094d5ccd81cc1b58da236cd21e48b206cdb236cc77c6f813b646c93ba46e811b2415996646606a75959c7596dccaae7ac0d9519615a51aec9bc5902c663334732399037e633347323990376e8ccd3cc8e640dd319af7c6f846644828263cc8df020d626269133df1be06a3a61ed313a45f69bb7c6f815ce897da47e013d84b3be39902b7e013d84c86897d84dfcc8df0350d22fb4c869c4d9be37c081509904123991cc819859389ab7c73206d9399a7991cc81b73135732399037663334f32399036e633356f8df0a9732b39eb36b3cacefd60457370888132622004444211110062221488884222202222148888422220222215126221083110a4888844c88880888854c18881a9e577ef1103ffd9',
                '1ad.png', null, null, 2558, 726860, now(), 'CONFIGURATION', false, NULL, 'PUBLIC', false, NULL, NULL, 'Produkt_ta_produkt18120358_');"""
        );

        List<Product> result = productService.getAllProducts().collectList().block();

        assertThat(result, hasSize(1));

        String path = new ExtendedURIBuilder(result.getFirst().getLogoUrl()).getPath();

        List<byte[]> bytesList = webTestClient.get()
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .returnResult(byte[].class)
                .getResponseBody()
                .collectList()
                .block();

        assertThat(bytesList, is(not(nullValue())));

        byte[] bytesFromWebClient = DataBufferUtils.joinByteArrayList(bytesList);

        Object bytesFromDatabase = databaseClient.sql("SELECT header_image FROM t_product WHERE id = 761392")
                .fetch()
                .one()
                .map(row -> row.get("header_image"))
                .block();

        assertThat((byte[]) bytesFromDatabase, is(bytesFromWebClient));

    }
}