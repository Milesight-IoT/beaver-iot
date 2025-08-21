<p align="center">
  <img alt="Beaver Iot Logo" width="400" src="beaver-iot.svg" />
</p>
<hr>

[![Discord](https://img.shields.io/discord/1270281746687983626.svg?label=Discord&logo=discord&colorB=7289da)](https://discord.gg/vNFxbwfErm)
[![Maven Central](https://img.shields.io/maven-central/v/com.milesight.beaveriot/beaver-iot.svg)](https://central.sonatype.com/search?q=beaver-iot&smo=true)
[![Docker Pulls](https://img.shields.io/docker/pulls/milesight/beaver-iot)](https://hub.docker.com/r/milesight/beaver-iot)
[![Publish Snapshot Package](https://github.com/Milesight-IoT/beaver-iot/actions/workflows/snapshot-publish.yml/badge.svg)](https://github.com/Milesight-IoT/beaver-iot/actions/workflows/snapshot-publish.yml)
[![Publish Release Package](https://github.com/Milesight-IoT/beaver-iot/actions/workflows/release-publish.yml/badge.svg)](https://github.com/Milesight-IoT/beaver-iot/actions/workflows/release-publish.yml)

# What is Beaver IoT?

Beaver IoT is an open-source platform designed for rapid and intuitive IoT application development. It supports the entire process from device connectivity to data processing, analysis, and visualization. Besides, Beaver IoT supports IoT developers to quickly build a variety of integrations based on project encapsulation capabilities, which is an ideal choice for IoT prototyping and scalable solutions. Beaver IoT fosters innovation and community collaboration in the IoT space.

# Features

- Monitor devices and collect real-time data
- Intuitive dashboards and rich widgets for data visualization
- Integrations with third-party platforms, devices and services
- [Multiple, scalable deployment using docker](https://www.milesight.com/beaver-iot/docs/user-guides/installation)
- Connecting devices across platforms through workflows
- User and role permissions functionality

# Start

This repository is the backend core of Beaver IoT and does not include integrations.

To start a Beaver IoT Application quickly, please refer to [Beaver IoT Documentation](https://www.milesight.com/beaver-iot/).

Related projects:
* [Beaver IoT Integrations](https://github.com/Milesight-IoT/beaver-iot-integrations)
* [Beaver IoT Frontend](https://github.com/Milesight-IoT/beaver-iot-web)
* [Beaver IoT Docker](https://github.com/Milesight-IoT/beaver-iot-docker)

# Build with Maven

* Java Version 17 SDK
```
mvn install -Dmaven.test.skip=true
```

