-- maakt geometry update scripts voor de rsgb wijk tabel tbv 113a_update_wijk_geom.sql
--
-- 1. laadt de wijk kaart uit de wijk en buurt kaart van CBS  in een postgis database
-- met gebruik shp2pgsql of shp2pgsql-gui
-- vereenvoudig de geometrie met:
--    update cbs_wijken2021 set geom = ST_Multi(ST_SimplifyPreserveTopology(geom, 0.01));
--    update cbs_wijken2021 set geom = ST_SnapToGrid(geom, 0.01);
--  (afronden cijfers achter de komma op 1cm
--
-- 2. gebruik onderstaande om de update scripts te maken voor 113a_update_wijk_geom.sql
SELECT

-- postgis blok
'UPDATE wijk SET geom = ST_GeomFromEWKT(''' || ST_AsEWKT(geom) || ''') WHERE code = ' || to_number(wk_code, '99999999') || ';
  ' || 'INSERT INTO wijk (code,geom) SELECT '|| to_number(wk_code, '99999999') ||',ST_GeomFromEWKT(''' || ST_AsEWKT(geom) || ''') WHERE NOT EXISTS (SELECT 1 FROM wijk WHERE code='||to_number(wk_code, '99999999')||');'
   as "--postgis wijk 2021 geometrie update"

-- oracle blok
-- NB. Oracle heeft handwerk nodig! 
-- todo/handwerk 
--   DECLARE wktA CLOB; wktB CLOB; wktC CLOB; wktD CLOB; wktE CLOB; wktF CLOB; wktG CLOB; wktH CLOB;wktJ CLOB; wktK CLOB;wktL CLOB;wktM CLOB;wktN CLOB;wktP CLOB;wktQ CLOB;wktR CLOB; BEGIN
--     aan het begin van het script plakken
-- todo/handwerk `END;` aan het eind van het export bestand plakken
-- Na het laden van de data kan het zinvol zijn om de ruimtelijke index te verversen; bijvoorbeeld ALTER INDEX WIJK_GEOM1_IDX REBUILD;

-- oracle heeft een max lengte van 32767 voor een string dus de wkt moet opgeknipt worden in stukken en dan als blob verwerkt...
-- dus kijk met "wc -L 113a_update_wijk_geom.sql" en/of "cat 113a_update_wijk_geom.sql|awk '{print length, $0}'|sort -nr|head -1" 
-- even of de regels niet te lang zijn (=< 32780). NB de max lengte voor sqlplus is veel lager (<2500 char per regel)
--'
--  wktA := ''' || substr(ST_AsText(geom), 0,32766)      || ''';
--  wktB := ''' || substr(ST_AsText(geom), 32766,32766)  || '@' || ''';
--  wktC := ''' || substr(ST_AsText(geom), 65532,32766)  || '@' || ''';
--  wktD := ''' || substr(ST_AsText(geom), 98298,32766)  || '@' || ''';
--  wktE := ''' || substr(ST_AsText(geom), 131064,32766) || '@' || ''';
--  wktF := ''' || substr(ST_AsText(geom), 163830,32766) || '@' || ''';
--  wktG := ''' || substr(ST_AsText(geom), 196596,32766) || '@' || ''';
--  wktH := ''' || substr(ST_AsText(geom), 229362,32766) || '@' || ''';
--  wktJ := ''' || substr(ST_AsText(geom), 262128,32766) || '@' || ''';
--  wktK := ''' || substr(ST_AsText(geom), 294894,32766) || '@' || ''';
--  wktL := ''' || substr(ST_AsText(geom), 327660,32766) || '@' || ''';
--  wktM := ''' || substr(ST_AsText(geom), 360426,32766) || '@' || ''';
--  wktN := ''' || substr(ST_AsText(geom), 393192,32766) || '@' || ''';
--  wktP := ''' || substr(ST_AsText(geom), 425958,32766) || '@' || ''';
--  wktQ := ''' || substr(ST_AsText(geom), 458724,32766) || '@' || ''';
--  wktR := ''' || substr(ST_AsText(geom), 491490,32766) || '@' || ''';
--  wktS := ''' || substr(ST_AsText(geom), 524256,32766) || '@' || ''';
--  wktT := ''' || substr(ST_AsText(geom), 557022,32766) || '@' || ''';
--  wktU := ''' || substr(ST_AsText(geom), 589788,32766) || '@' || ''';
--  wktV := ''' || substr(ST_AsText(geom), 622554,32766) || '@' || ''';
--  wktW := ''' || substr(ST_AsText(geom), 655320,32766) || '@' || ''';
--  wktX := ''' || substr(ST_AsText(geom), 688086,32766) || '@' || ''';
--  wktY := ''' || substr(ST_AsText(geom), 720852,32766) || '@' || ''';
--  wktZ := ''' || substr(ST_AsText(geom), 753618,32766) || '@' || ''';
--  wktAA := ''' || substr(ST_AsText(geom), 786384,32766) || '@' || ''';
--  wktAB := ''' || substr(ST_AsText(geom), 819150,32766) || '@' || ''';
--  wktAC := ''' || substr(ST_AsText(geom), 851916,32766) || '@' || ''';
--  wktAD := ''' || substr(ST_AsText(geom), 884682) || '@' || ''';
--
--  DBMS_LOB.APPEND(wktA,wktB);
--  DBMS_LOB.APPEND(wktA,wktC);
--  DBMS_LOB.APPEND(wktA,wktD);
--  DBMS_LOB.APPEND(wktA,wktE);
--  DBMS_LOB.APPEND(wktA,wktF);
--  DBMS_LOB.APPEND(wktA,wktG);
--  DBMS_LOB.APPEND(wktA,wktH);
--  DBMS_LOB.APPEND(wktA,wktJ);
--  DBMS_LOB.APPEND(wktA,wktK);
--  DBMS_LOB.APPEND(wktA,wktL);
--  DBMS_LOB.APPEND(wktA,wktM);
--  DBMS_LOB.APPEND(wktA,wktN);
--  DBMS_LOB.APPEND(wktA,wktP);
--  DBMS_LOB.APPEND(wktA,wktQ);
--  DBMS_LOB.APPEND(wktA,wktR);
--  DBMS_LOB.APPEND(wktA,wktS);
--  DBMS_LOB.APPEND(wktA,wktT);
--  DBMS_LOB.APPEND(wktA,wktU);
--  DBMS_LOB.APPEND(wktA,wktV);
--  DBMS_LOB.APPEND(wktA,wktW);
--  DBMS_LOB.APPEND(wktA,wktX);
--  DBMS_LOB.APPEND(wktA,wktY);
--  DBMS_LOB.APPEND(wktA,wktZ);
--  DBMS_LOB.APPEND(wktA,wktAA);
--  DBMS_LOB.APPEND(wktA,wktAB);
--  DBMS_LOB.APPEND(wktA,wktAC);
--  DBMS_LOB.APPEND(wktA,wktAD);
--  wktA := REPLACE(wktA,''@'','''');
--
--  MERGE INTO wijk USING dual ON (CODE=' || to_number(wk_code, '99999999') || ')
--    WHEN MATCHED THEN UPDATE SET GEOM=SDO_GEOMETRY(wktA,28992)
--    WHEN NOT MATCHED THEN INSERT (CODE,GEOM) VALUES (' || to_number(wk_code, '99999999') || ', SDO_GEOMETRY(wktA,28992));'
--as "--oracle wijk 2021 geometrie update"

FROM cbs_wijken2021
WHERE water = 'NEE'
--AND wk_code IN ('WK000300','WK000500','WK999999','WK005902', 'WK192499', 'WK067706')
ORDER BY wk_code;
