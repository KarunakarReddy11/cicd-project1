pipeline {
    agent any

    environment {
        DOCKER_HUB_CRED_ID = 'dockerhub-credentials'
        DOCKER_HUB_REPO    = 'karunakar11/devops-java-app'
        IMAGE_TAG          = "${BUILD_NUMBER}"
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
                // Verify pom.xml exists in the current Jenkins workspace
                sh 'ls -la pom.xml'
            }
        }

        stage('Gitleaks Secret Scanning') {
            steps {
                echo 'Checking for committed secrets...'
                // Run secret scanner directly on files present in the workspace
                sh '''
                    docker run --rm \
                      -v /var/lib/docker/volumes/jenkins_home/_data/workspace/${JOB_NAME}:/path \
                      zricethezav/gitleaks:latest \
                      detect --source="/path" --verbose --no-git || true
                '''
            }
        }

        stage('Hadolint Dockerfile Linting') {
            steps {
                echo 'Linting Dockerfile...'
                // Pipe Dockerfile directly via stdin so no volume mounts are needed
                sh 'docker run --rm -i hadolint/hadolint:latest < Dockerfile || true'
            }
        }

        stage('Docker Image Build & Unit Test') {
            steps {
                echo "Building application container image: ${DOCKER_HUB_REPO}:${IMAGE_TAG}"
                // The multi-stage Dockerfile handles compilation and packaging cleanly
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
            echo "CI pipeline failed at Build #${BUILD_NUMBER}. Check console output."
        }
    }
}
