
Pod::Spec.new do |s|
  s.name         = "RNReadiumReactBridge"
  s.version      = "1.0.0"
  s.summary      = "RNReadiumReactBridge"
  s.description  = <<-DESC
                  RNReadiumReactBridge
                   DESC
  s.homepage     = ""
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNReadiumReactBridge.git", :tag => "master" }
  s.source_files  = "RNReadiumReactBridge/**/*.{h,m,swift}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  