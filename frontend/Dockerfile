# Build stage
FROM ghcr.io/cirruslabs/flutter:3.38.9 AS build

WORKDIR /app
COPY pubspec.yaml ./
RUN flutter pub get

COPY . .
RUN rm -rf .dart_tool build
RUN flutter pub get
RUN flutter build web --release --dart-define=BASE_URL=/api

# Serve stage
FROM nginx:alpine
COPY --from=build /app/build/web /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
