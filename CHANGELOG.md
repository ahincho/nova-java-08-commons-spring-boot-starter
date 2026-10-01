# Changelog

## [3.0.1](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/compare/v3.0.0...v3.0.1) (2026-10-01)


### Bug Fixes

* register the envelope records for native images ([09a687b](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/09a687bdb72a6507591f6cf9926b46db4a8df874))

## [3.0.0](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/compare/v2.0.0...v3.0.0) (2026-10-01)


### ⚠ BREAKING CHANGES

* error codes follow the platform catalog instead of ERROR, validation errors use BAD_REQUEST instead of VALIDATION_ERROR, an IllegalArgumentException is a platform error answered as 500, generic messages are the Spanish ones of the catalog, and ErrorCodes and GlobalExceptionHandler.envelope are gone. The README has the recipe.

### Features

* answer errors with the layered model of ADR-031 ([89c0b6a](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/89c0b6aaeb7a8ba047883749ee03ce7d5a0fc163))


### Bug Fixes

* answer an error envelope when a controller returns 4xx or 5xx ([96675e4](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/96675e49556d96cff1512d8f3f0b53510509b29f))
* give each API error the code of its status ([6b6d27a](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/6b6d27a89c603abe67fb734c58918e07481cf5e2))
* leave actuator, the error controller and raw bodies unwrapped ([888bb4a](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/888bb4a947bd49704c5ce8c30804c40ab541c24a))
* map Spring MVC exceptions to their own 4xx status ([9122eac](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/9122eacd9992c2e19e607a92cde575a0308bb56c))
* write the real status in the API envelope ([339ad3b](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/339ad3bbde4525ebfcadfdc7075af2867f1821f9))

## [2.0.0](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/compare/v1.0.2...v2.0.0) (2026-09-27)


### ⚠ BREAKING CHANGES

* pe.edu.nova.java.starters:nova-api-standard-starter becomes nova-api-standard-spring-boot-starter and nova-mask-starter becomes nova-mask-spring-boot-starter; consumers migrate with ops/rename-artifacts.py --phase 2 from nova-shared-01-docs.

### Features

* publish the starters with spring-boot in their artifactId ([cb9c4ae](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/cb9c4ae7bd209866b1a6fbc75731c7cbfcc0c429))


### Bug Fixes

* **deps:** move to Spring Boot 4.0.8 and patch what OWASP flags ([71ee36a](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/71ee36a2a7eb49d2b72b120fdb6ac80ccf8ff005))
* **deps:** move to Spring Boot 4.0.8 and patch what OWASP flags ([56833d2](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/56833d2a97d681607ac65a86353fc4ccc6579abb))

## [1.0.2](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/compare/v1.0.1...v1.0.2) (2026-09-27)


### Documentation

* add a README and adopt EPL-2.0 ([dc00322](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter/commit/dc00322af548dc32f5e744493ec1f7e46880eeae))

## [1.0.1](https://github.com/ahincho/nova-java-commons-spring-boot-starter/compare/v1.0.0...v1.0.1) (2026-07-13)


### Bug Fixes

* **ci:** add component + skip-snapshot + manifest-file (mask-utils pattern) ([eb9968e](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/eb9968e7a6d543964c0551c9c08dd06034f97227))
* **ci:** add last-release-sha, include-component-in-tag: false, release-type: java to top-level config; pass manifest-file in wrapper ([bae97e6](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/bae97e6282ccf3c41997d5bb8bd96d3c24e00474))

## 1.0.0 (2026-07-10)


### Features

* **ci:** migrate to release-please + tag-based publish flow (NOVA-SEMVER-13) ([61855a4](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/61855a46789cdf375cdfbd05e071398a33fda3ad))
* **gradle:** add GPG signing plugin for Maven Central publishing (NOVA-SEMVER-10) ([4791a0c](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/4791a0c5ed67dcb17b439adf3236e0c218c6962e))
* **gradle:** enable Local Build Cache and Configuration Cache (NOVA-SEMVER-23-24) ([2f798c5](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/2f798c5693c026794653c0cbb04d12d8b7fd814c))
* initial commit - Spring Boot starter that aggregates mask-utils, api-standard, observability ([3302dcf](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/3302dcf539d0d52049c94893531e8a87741bc5b4))


### Bug Fixes

* **ci:** inline publish-on-tag and remove dirty closure for Gradle 9.6.1 ([1847255](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/18472556469229af8594bfbfef1d949272b9dccd))
* **ci:** use PAT fallback for release-please to enable tag-triggered workflows ([7de40b1](https://github.com/ahincho/nova-java-commons-spring-boot-starter/commit/7de40b1370532c76b831c76eac3d8438de7e3319))
