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

desc "Compile, build docker image and run locally"
task :rundockerlocal do
  command "./mvnw clean install spring-boot:build-image -Dspring-boot.build-image.imageName=springio/gs-spring-boot-docker"
  command "docker run -p 8080:8080 -t springio/gs-spring-boot-docker" 
end
