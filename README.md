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