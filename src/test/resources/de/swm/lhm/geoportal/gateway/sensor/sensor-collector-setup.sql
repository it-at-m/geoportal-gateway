DELETE FROM t_plugin_sta_sensor;

INSERT INTO t_plugin_sta_sensor
    (id, name, url, stageless_id, stage)
VALUES
    -- %s will be set via de.swm.lhm.geoportal.gateway.sensor.SensorRoutesCollectorTestConfig.initializer
    -- port = de.swm.lhm.geoportal.gateway.sensor.SensorRoutesCollectorTest.FROST_PORT
    ('2e493acd-1551-4bcb-b8bb-b96f05290b87', 'layer1', 'http://localhost:%s/frost', 'eca0dc07-aa9c-4053-852c-d7ba02ed4e8f', 'CONFIGURATION');