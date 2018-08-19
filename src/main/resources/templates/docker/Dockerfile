FROM gcr.io/distroless/java:debug

COPY @@FILES@@ /app/

ENTRYPOINT ["java", "-cp", "/app/:/app/*", "@@MAINCLASS@@", "@@ARG@@"]
