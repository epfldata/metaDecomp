FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl wget git build-essential clang cmake ninja-build \
    maven python3 python3-venv jq \
    latexmk texlive-plain-generic texlive-latex-extra texlive-science texlive-publishers texlive-fonts-recommended \
    texlive-fonts-extra fonts-linuxlibertine \
    && rm -rf /var/lib/apt/lists/*
# Match the Oracle GraalVM version used on the author's Mac.
RUN arch=$(uname -m); if [ "$arch" = x86_64 ]; then arch=x64; fi; \
    mkdir -p /opt/graalvm && curl -fL "https://download.oracle.com/graalvm/17/archive/graalvm-jdk-17.0.12_linux-${arch}_bin.tar.gz" \
    | tar -xz --strip-components=1 -C /opt/graalvm
ENV JAVA_HOME=/opt/graalvm
ENV PATH="${JAVA_HOME}/bin:${PATH}"
# sbt uses the version pinned in project/build.properties.
RUN curl -fL https://github.com/sbt/sbt/releases/download/v1.10.2/sbt-1.10.2.tgz \
    | tar -xz -C /opt && ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt
ENV CC=clang CXX=clang++
WORKDIR /workspace
ENTRYPOINT ["bash", "reproduce.sh"]
