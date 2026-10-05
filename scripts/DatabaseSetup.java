import java.sql.*;
import java.util.*;

/** Creates only the four application databases; schema changes are performed by Flyway. */
class DatabaseSetup {
    public static void main(String[] args) throws Exception {
        String url="jdbc:postgresql://"+System.getenv("DATABASE_HOST")+":26257/defaultdb?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory&connectTimeout=15&socketTimeout=30";
        try(Connection c=DriverManager.getConnection(url,System.getenv("DB_USERNAME"),System.getenv("DB_PASSWORD")); Statement s=c.createStatement()) {
            Set<String> existing=new HashSet<>(); try(ResultSet r=s.executeQuery("SHOW DATABASES")) { while(r.next()) existing.add(r.getString(1)); }
            for(String database:List.of("users_db","products_db","orders_db","invoices_db")) {
                if(existing.contains(database)) {
                    try(ResultSet r=s.executeQuery("SELECT count(*) FROM "+database+".information_schema.tables WHERE table_schema='public'")) {
                        r.next(); if(r.getLong(1)>0) {
                            try(ResultSet tracked=s.executeQuery("SELECT count(*) FROM "+database+".information_schema.tables WHERE table_schema='public' AND table_name='flyway_schema_history'")) {
                                tracked.next(); if(tracked.getLong(1)==0) throw new IllegalStateException(database+" already contains unmanaged tables; refusing to reuse it");
                            }
                        }
                    }
                    System.out.println(database+": existing database retained");
                } else { s.execute("CREATE DATABASE "+database); System.out.println(database+": created"); }
            }
        }
    }
}
