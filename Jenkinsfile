pipeline {
    agent any

    environment {
        // 部署目录在宿主机上，Jenkins 容器已挂载到 /workspace/demo
        DEPLOY_DIR = '/workspace/demo'
        // 用容器跑构建，不污染 Jenkins 本身
        MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-17'
        NODE_IMAGE = 'node:20-alpine'
        // Maven 依赖缓存，避免每次重新下载
        MVN_CACHE = '/var/jenkins_home/.m2'
        // 前端依赖缓存
        NPM_CACHE = '/var/jenkins_home/.npm'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'echo "当前提交: $(git rev-parse --short HEAD)"; git log -1 --pretty=format:"%h %s"'
            }
        }

        stage('Build Backend') {
            steps {
                sh """
                    docker run --rm \
                      -v "\$PWD/backend:/src" \
                      -v "${MVN_CACHE}:/root/.m2" \
                      -w /src \
                      ${MAVEN_IMAGE} \
                      mvn -B clean package -DskipTests
                """
            }
            post {
                success {
                    sh 'ls -lh backend/target/*.jar'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                sh """
                    docker run --rm \
                      -v "\$PWD/frontend:/src" \
                      -v "${NPM_CACHE}:/root/.npm" \
                      -w /src \
                      ${NODE_IMAGE} \
                      sh -c "npm ci && npm run build"
                """
            }
            post {
                success {
                    sh 'ls -lh frontend/dist'
                    sh 'ls -lh frontend/dist/assets'
                }
            }
        }

        stage('Deploy Frontend') {
            steps {
                // Nginx 直接读磁盘，覆盖文件即可，不需要重启
                sh """
                    mkdir -p ${DEPLOY_DIR}/web/dist
                    rm -rf ${DEPLOY_DIR}/web/dist/*
                    cp -r frontend/dist/* ${DEPLOY_DIR}/web/dist/
                    ls -lh ${DEPLOY_DIR}/web/dist
                """
            }
        }

        stage('Deploy Backend') {
            steps {
                sh """
                    cp backend/target/demo-backend-1.0.0.jar ${DEPLOY_DIR}/app/app.jar
                    cd ${DEPLOY_DIR}
                    docker compose up -d --build backend
                """
            }
        }

        stage('Verify') {
            steps {
                sh '''
                    set -e
                    echo "等待后端就绪…"
                    for i in $(seq 1 30); do
                      if curl -sf http://demo-web/api/health > /dev/null 2>&1; then
                        echo "健康检查通过"
                        curl -s http://demo-web/api/health
                        exit 0
                      fi
                      sleep 2
                    done
                    echo "健康检查超时，输出后端日志"
                    docker logs --tail 50 demo-backend
                    exit 1
                '''
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f > /dev/null 2>&1 || true'
        }
        failure {
            echo '构建或部署失败，请查看上方日志'
        }
    }
}
