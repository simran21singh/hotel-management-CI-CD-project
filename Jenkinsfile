pipeline {
    agent any

    // -------------------------------------------------------------------
    // Environment Variables (Dynamic, no hardcoded secrets)
    // -------------------------------------------------------------------
    environment {
        APP_NAME          = 'hotel-management-system'
        IMAGE_NAME        = 'hotel-management-app'
        APP_PORT          = '8080'
        HEALTH_ENDPOINT   = 'http://localhost:8080/actuator/health'
        DOCKER_COMPOSE_CMD = 'docker-compose'
    }

    // -------------------------------------------------------------------
    // Build Options
    // -------------------------------------------------------------------
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 20, unit: 'MINUTES')
        timestamps()
    }

    // -------------------------------------------------------------------
    // Pipeline Stages
    // -------------------------------------------------------------------
    stages {

        // ===============================================================
        // STAGE 1: Source Code Checkout
        // ===============================================================
        stage('Checkout') {
            steps {
                echo '=================================================='
                echo ' STAGE 1: CHECKOUT - Pulling source code from Git '
                echo '=================================================='
                // Checks out code from the configured GitHub repository branch
                checkout scm
                script {
                    echo "Checked out commit: ${env.GIT_COMMIT ?: 'Local Workspace'}"
                    echo "Branch: ${env.GIT_BRANCH ?: 'main'}"
                }
            }
        }

        // ===============================================================
        // STAGE 2: Maven Compilation
        // ===============================================================
        stage('Maven Build') {
            steps {
                echo '=================================================='
                echo ' STAGE 2: MAVEN BUILD - Compiling source files    '
                echo '=================================================='
                script {
                    if (isUnix()) {
                        sh 'mvn clean compile'
                    } else {
                        bat 'mvn clean compile'
                    }
                }
                echo 'Compilation completed successfully.'
            }
        }

        // ===============================================================
        // STAGE 3: Automated Unit & Integration Testing
        // ===============================================================
        stage('Unit Testing') {
            steps {
                echo '=================================================='
                echo ' STAGE 3: TESTING - Executing JUnit & Mockito     '
                echo '=================================================='
                script {
                    try {
                        if (isUnix()) {
                            sh 'mvn test'
                        } else {
                            bat 'mvn test'
                        }
                    } catch (err) {
                        echo "[ERROR] Automated tests failed! Halting pipeline to prevent faulty deployment."
                        throw err
                    }
                }
            }
            post {
                always {
                    // Publishes test results even if tests fail so reports can be inspected
                    junit allowEmptyResults: false, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ===============================================================
        // STAGE 4: Package Artifact (Executable JAR)
        // ===============================================================
        stage('Package') {
            steps {
                echo '=================================================='
                echo ' STAGE 4: PACKAGE - Packaging Spring Boot fat JAR '
                echo '=================================================='
                script {
                    // Tests already verified in Stage 3, skip to optimize package step
                    if (isUnix()) {
                        sh 'mvn package -DskipTests'
                    } else {
                        bat 'mvn package -DskipTests'
                    }
                }
                // Archive the generated JAR artifact in Jenkins
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                echo 'Application successfully packaged and archived.'
            }
        }

        // ===============================================================
        // STAGE 5: Build & Tag Docker Image
        // ===============================================================
        stage('Docker Image Build') {
            steps {
                echo '=================================================='
                echo ' STAGE 5: DOCKER BUILD - Building Container Image '
                echo '=================================================='
                script {
                    def imageTag = "${env.IMAGE_NAME}:${env.BUILD_NUMBER}"
                    def latestTag = "${env.IMAGE_NAME}:latest"
                    
                    echo "Building Docker image: ${imageTag} and ${latestTag}"
                    if (isUnix()) {
                        sh "docker build -t ${imageTag} -t ${latestTag} ."
                    } else {
                        bat "docker build -t ${imageTag} -t ${latestTag} ."
                    }
                }
                echo 'Docker image build and tagging successful.'
            }
        }

        // ===============================================================
        // STAGE 6: Continuous Deployment (Docker Compose)
        // ===============================================================
        stage('Deployment') {
            steps {
                echo '=================================================='
                echo ' STAGE 6: DEPLOYMENT - Orchestrating Containers   '
                echo '=================================================='
                script {
                    echo 'Deploying application stack with Docker Compose...'
                    // Do NOT pass -v flag to preserve named volume 'mysql_data'
                    if (isUnix()) {
                        sh 'docker-compose up --build -d'
                    } else {
                        bat 'docker-compose up --build -d'
                    }
                }
                echo 'Containers deployed in detached background mode.'
            }
        }

        // ===============================================================
        // STAGE 7: Smoke Test & Health Check Verification
        // ===============================================================
        stage('Smoke Test / Health Check') {
            steps {
                echo '=================================================='
                echo ' STAGE 7: SMOKE TEST - Probing Actuator Health    '
                echo '=================================================='
                script {
                    echo "Waiting for Spring Boot and MySQL to reach UP status on ${env.HEALTH_ENDPOINT}..."
                    
                    def isHealthy = false
                    def maxRetries = 15
                    def delaySeconds = 6

                    for (int attempt = 1; attempt <= maxRetries; attempt++) {
                        echo "Health check attempt ${attempt}/${maxRetries}..."
                        try {
                            if (isUnix()) {
                                def responseCode = sh(
                                    script: "curl -s -o /dev/null -w '%{http_code}' ${env.HEALTH_ENDPOINT}",
                                    returnStdout: true
                                ).trim()

                                if (responseCode == "200") {
                                    isHealthy = true
                                    echo "HTTP 200 OK received from ${env.HEALTH_ENDPOINT}!"
                                    break
                                }
                            } else {
                                def statusCode = bat(
                                    script: "@powershell -Command \"try { (Invoke-WebRequest -Uri '${env.HEALTH_ENDPOINT}' -UseBasicParsing).StatusCode } catch { 500 }\"",
                                    returnStdout: true
                                ).trim()

                                if (statusCode.contains("200")) {
                                    isHealthy = true
                                    echo "HTTP 200 OK received from ${env.HEALTH_ENDPOINT}!"
                                    break
                                }
                            }
                        } catch (Exception ex) {
                            echo "Service not ready yet: ${ex.message}"
                        }
                        sleep(time: delaySeconds, unit: 'SECONDS')
                    }

                    if (!isHealthy) {
                        error("Deployment verification FAILED: Health endpoint did not return HTTP 200 within expected timeframe.")
                    }
                    
                    echo "=================================================="
                    echo " SUCCESS: Application is healthy and responsive!  "
                    echo " Dashboard URL: http://localhost:${env.APP_PORT}/ "
                    echo "=================================================="
                }
            }
        }
    }

    // -------------------------------------------------------------------
    // Post-Build Pipeline Actions
    // -------------------------------------------------------------------
    post {
        always {
            echo "Pipeline run completed for build #${env.BUILD_NUMBER}."
        }
        success {
            echo "========================================================"
            echo "  CI/CD PIPELINE SUCCEEDED!                             "
            echo "  All tests passed, Docker images built, and containers "
            echo "  are live at http://localhost:${env.APP_PORT}/         "
            echo "========================================================"
        }
        failure {
            echo "========================================================"
            echo "  PIPELINE FAILED! Deployment aborted.                  "
            echo "  Inspect test reports and build console output above.  "
            echo "========================================================"
        }
        unstable {
            echo "Pipeline marked as UNSTABLE. Review warnings."
        }
    }
}
