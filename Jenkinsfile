pipeline {
    agent any

    environment {
        DOCKER_HUB_CRED_ID = 'dockerhub-credentials'
        DOCKER_HUB_REPO    = 'YOUR_DOCKERHUB_USERNAME/devops-java-app'
        IMAGE_TAG          = "${BUILD_NUMBER}"
        WORKSPACE_DIR      = "${env.WORKSPACE}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                checkout scm
                sh 'git log -1 --pretty=format:"Commit: %h by %an on %cd | %s"'
            }
        }

        stage('Maven Compile & Unit Test') {
            steps {
                echo 'Running unit tests and code compilation...'
                sh '''
                    docker run --rm \
                      -v "${WORKSPACE_DIR}":/app \
                      -w /app \
                      maven:3.9.6-eclipse-temurin-17-alpine \
                      mvn clean test
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Gitleaks Secret Scanning') {
            steps {
                echo 'Scanning repository for leaked credentials and API tokens...'
                sh '''
                    docker run --rm \
                      -v "${WORKSPACE_DIR}":/path \
                      zricethezav/gitleaks:latest \
                      detect --source="/path" --verbose --no-git || true
                '''
            }
        }

        stage('Hadolint Dockerfile Linting') {
            steps {
                echo 'Linting Dockerfile for container best practices...'
                sh '''
                    docker run --rm -i \
                      hadolint/hadolint:latest < Dockerfile || true
                '''
            }
        }

        stage('Trivy Filesystem Vulnerability Scan') {
            steps {
                echo 'Auditing filesystem and dependencies for known CVEs...'
                sh '''
                    docker run --rm \
                      -v "${WORKSPACE_DIR}":/root/project \
                      aquasec/trivy:latest \
                      fs --exit-code 0 --severity HIGH,CRITICAL /root/project
                '''
            }
        }

        stage('Docker Image Build') {
            steps {
                echo "Building application container image: ${DOCKER_HUB_REPO}:${IMAGE_TAG}"
                sh '''
                    docker build -t ${DOCKER_HUB_REPO}:${IMAGE_TAG} -t ${DOCKER_HUB_REPO}:latest .
                '''
            }
        }

        stage('Trivy Container Image Scan') {
            steps {
                echo "Scanning Docker image ${DOCKER_HUB_REPO}:${IMAGE_TAG} for container vulnerabilities..."
                sh '''
                    docker run --rm \
                      -v /var/run/docker.sock:/var/run/docker.sock \
                      aquasec/trivy:latest \
                      image --exit-code 0 --severity HIGH,CRITICAL \
                      --no-progress \
                      --scanners vuln \
                      ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                '''
            }
        }

        stage('Docker Hub Login & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CRED_ID}", usernameVariable: 'DH_USER', passwordVariable: 'DH_PASS')]) {
                    sh '''
                        echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                        docker push ${DOCKER_HUB_REPO}:${IMAGE_TAG}
                        docker push ${DOCKER_HUB_REPO}:latest
                    '''
                }
            }
        }

        stage('Cleanup Local Docker Images') {
            steps {
                sh '''
                    docker rmi -f ${DOCKER_HUB_REPO}:${IMAGE_TAG} || true
                    docker rmi -f ${DOCKER_HUB_REPO}:latest || true
                '''
            }
        }
    }

    post {
        success {
            echo "CI pipeline completed successfully for Build #${BUILD_NUMBER}."
        }
        failure {
            echo "CI pipeline failed at Build #${BUILD_NUMBER}. Check logs for details."
        }
    }
}