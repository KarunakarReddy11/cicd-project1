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
                sh 'ls -la pom.xml'
            }
        }

        stage('Gitleaks Secret Scanning') {
            steps {
                echo 'Checking for committed secrets...'
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
                sh 'docker run --rm -i hadolint/hadolint:latest < Dockerfile || true'
            }
        }

        stage('Docker Image Build & Unit Test') {
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
                      -v /var/lib/docker/volumes/jenkins_home/_data/trivy-cache:/root/.cache/ \
                      aquasec/trivy:latest \
                      image \
                      --scanners vuln \
                      --pkg-types os \
                      --exit-code 0 \
                      --severity HIGH,CRITICAL \
                      --no-progress \
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

        // ==========================================
        // NEW STAGE: Trigger Ansible Deployment
        // ==========================================
        stage('Trigger Ansible Deployment') {
            steps {
                echo "Executing Ansible deployment for build #${IMAGE_TAG}..."
                sh """
                    docker exec ansible-controller ansible-playbook /etc/ansible/deploy.yml \
                      --extra-vars "DOCKER_REPO=${DOCKER_HUB_REPO} BUILD_TAG=${IMAGE_TAG}"
                """
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
            echo "CI/CD Pipeline succeeded: Application deployed to target server with tag ${IMAGE_TAG}."
        }
        failure {
            echo "Pipeline failed during execution. Inspect build console output."
        }
    }
}
