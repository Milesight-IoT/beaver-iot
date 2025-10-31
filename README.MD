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

Beaver IoT is an open-source platform that empowers individuals and businesses to easily connect, monitor, and control their smart devices. With Beaver IoT, users can manage all their devices in one place, view real-time data, and gain valuable insights through intuitive dashboards and visualizations. Whether at home or in the workplace, Beaver IoT makes it simple to automate routines, enhance security, and improve efficiency. Its user-friendly design ensures that anyone can enjoy the benefits of a smart, connected environment.

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

**[Beaver IoT Integrations](https://github.com/Milesight-IoT/beaver-iot-integrations)**  
Beaver IoT Integrations serve as the main bridge for connecting Beaver IoT with third-party services, devices, and platforms.

**[Beaver IoT Frontend](https://github.com/Milesight-IoT/beaver-iot-web)**  
Beaver IoT Frontend provides a modern, user-friendly web interface for managing devices, visualizing data, and configuring automation within the Beaver IoT platform.

**[Beaver IoT Docker](https://github.com/Milesight-IoT/beaver-iot-docker)**  
Beaver IoT Docker offers containerized deployment solutions for the Beaver IoT platform. With ready-to-use Docker images and configuration templates, users can quickly set up and run Beaver IoT in various environments, ensuring flexibility and scalability for different use cases.

**[Beaver IoT Blueprint](https://github.com/Milesight-IoT/beaver-iot-blueprint)**  
Beaver IoT Blueprint includes templates for various device models, making it easier to add support for new devices. To integrate additional device types, simply contribute corresponding templates and encoding/decoding scripts to this project, enabling broader compatibility and customization.

# Online Demo

* **[Demo Link](https://eu.demo.beaver-iot.com/auth/login)**
* **Test Username:** beaverdemo@milesight.com
* **Test Password:** Beaverdemo25

# Build with Maven

* Java Version 17 SDK
```
mvn install -Dmaven.test.skip=true
```

