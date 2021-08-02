require 'open3'
require 'digest/md5'

def command(cmd)
  puts "$ #{cmd}"

  Open3.popen2e(cmd) do |stdin, stdout_err, wait_thr|
    while line = stdout_err.gets
      puts line
    end

    exit_status = wait_thr.value
    abort "FAILED: #{cmd}" unless exit_status.success?
  end
end

def md5_digest_of_codebuid_codecommit_version
    if ENV['CODEBUILD_RESOLVED_SOURCE_VERSION'].nil?
        "latest"
    else
        Digest::MD5.hexdigest(ENV['CODEBUILD_RESOLVED_SOURCE_VERSION'])
    end
end

desc "Build runnable jar"
task :build do
  command "chmod +x ./mvnw && ./mvnw install"
end

desc "Run locally"
task :run => :build do
  command "java -jar ./target/spring-fargate-pipeline-cdk-0.0.1-SNAPSHOT.jar" 
end

desc "cdk ls"
task :cdkls => :build do
  command "cdk ls" 
end

desc "cdk deploy Pipeline"
task :cdkdeploypipeline => :build do
  command "cdk deploy --require-approval=never --path-metadata false --outputs-file pipeline.json -e Pipeline" 
end

desc "Build docker image"
task :dockerbuild do
  command "chmod +x ./mvnw && ./mvnw install spring-boot:build-image -Dspring-boot.build-image.imageName=furthermore/springdemo"
end

desc "Run in docker"
task :dockerrun => :dockerbuild do
  command "docker run -p 8080:8080 -t furthermore/springdemo" 
end

desc "Push docker image (expects DOCKER_REPOSITORY_URI and optionally CODEBUILD_RESOLVED_SOURCE_VERSION)"
task :dockerpush => :dockerbuild do
  command "aws ecr get-login-password | docker login --username AWS --password-stdin $DOCKER_REPOSITORY_URI"
  command "docker tag furthermore/springdemo $DOCKER_REPOSITORY_URI:#{ md5_digest_of_codebuid_codecommit_version }"
  command "docker push $DOCKER_REPOSITORY_URI:#{ md5_digest_of_codebuid_codecommit_version }"
end

desc "cdk deploy FargateDev (expects DOCKER_REPOSITORY_URI and DOCKER_REPOSITORY_ARN and optionally CODEBUILD_RESOLVED_SOURCE_VERSION)"
task :cdkdeployfargatedev => :build do
  command "cdk deploy --require-approval=never --path-metadata false --parameters dockerImageVersion=#{ md5_digest_of_codebuid_codecommit_version } --parameters repositoryArn=$DOCKER_REPOSITORY_ARN --parameters repositoryUri=$DOCKER_REPOSITORY_URI --outputs-file fargatedev.json -e FargateDev" 
end

desc "tar sources"
task :tarsources do
  command "jar cvf sources.zip .mvn/ Rakefile cdk.json mvnw pom.xml src/" 
end

desc "Upload sources to trigger pipeline"
task :uploadsourcespipeline => :tarsources do
  command "aws s3 cp sources.zip s3://$(aws cloudformation describe-stacks --stack-name Pipeline --query \"Stacks[0].Outputs[?OutputKey=='SourceBucketName'].OutputValue\" --output text)/" 
end

desc "Invoke deployed service endpoint"
task :curl do
  command "curl -s -v http://$(aws cloudformation describe-stacks --stack-name FargateDev --query \"Stacks[0].Outputs[?OutputKey=='LoadBalancerDnsName'].OutputValue\" --output text)/" 
end

desc "Delete FargateDev stack"
task :deletefargatedev do
  command "aws cloudformation delete-stack --stack-name FargateDev" 
end

desc "Delete Pipeline stack"
task :deletepipeline do
  command "aws cloudformation delete-stack --stack-name Pipeline" 
end
