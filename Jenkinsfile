node() {

    stage('Checkout') {
        checkout scm
    }
    
    stage('Build') {
        sh './gradlew clean assembleRelease'
        //lock('emulator') {
        //   sh './gradlew connectedCheck'
        //}
    }

    //stage('Archive') {
    //  archiveArtifacts 'app/build/outputs/apk/*'
    //}
}
