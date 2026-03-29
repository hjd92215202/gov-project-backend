docker run -d `
 -p 9000:9000 `
-p 9001:9001 `
 --name gov-minio `
-e "MINIO_ROOT_USER=govadmin" `
 -e "MINIO_ROOT_PASSWORD=govadminpassword" `
-v c:\minio\data:/data `
 minio/minio:RELEASE.2023-08-09T23-30-22Z `
server /data --console-address ":9001"



docker run --name opengauss --privileged=true -d -e GS_PASSWORD=Egov@123 -p 8888:5432 -v c:\opengauss:/var/lib/opengauss opengauss/opengauss:latest


CREATE USER db_user WITH PASSWORD 'Egov@123';
ALTER USER db_user SYSADMIN;


http://localhost:8080/api/doc.html
http://localhost:9001/login

docker run --name mdb1 `
  -v c:\mariadb\data:/var/lib/mysql `
  -p 13306:3306 `
  -e MYSQL_ROOT_PASSWORD=123 `
  -e MYSQL_USER=db_user `
  -e MYSQL_PASSWORD=Egov@123 `
  -d mariadb:10.11 `
  --character-set-server=utf8mb4 `
  --collation-server=utf8mb4_unicode_ci `
  --lower_case_table_names=1

v1
