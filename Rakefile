require 'open3'

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

desc "Build"
task :build do
  command "./mvnw install"
end


desc "Run locally"
task :run => :build do
  command "java -jar ./target/spring-fargate-pipeline-cdk-0.0.1-SNAPSHOT.jar" 
end

desc "Build docker image"
task :dockerbuild do
  command "./mvnw install spring-boot:build-image -Dspring-boot.build-image.imageName=springio/gs-spring-boot-docker"
end

desc "Run in docker"
task :dockerrun => :dockerbuild do
  command "docker run -p 8080:8080 -t springio/gs-spring-boot-docker" 
end

desc "tar sources"
task :tarsources do
  command "jar cvf sources.zip .mvn/ Rakefile cdk.json mvnw pom.xml src/" 
end

desc "cdk ls"
task :cdkls => :build do
  command "cdk ls" 
end

desc "cdk deploy BuildStack"
task :cdkdeploybuildstack => :build do
  command "cdk deploy --require-approval=never --path-metadata false --outputs-file buildstack.json -e BuildStack" 
end

desc "Upload sources"
task :uploadsources => :tarsources do
  command "aws s3 cp sources.zip s3://$(cat buildstack.json | jq -r '.BuildStack.SourceBucketName')/" 
end

desc "Trigger remote build"
task :remotebuild => :uploadsources do
   command "echo FIXME"
end
