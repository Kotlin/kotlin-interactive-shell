class {{brewFormulaName}} < Formula
  desc "Extendable Kotlin interactive shell with code completion, and other features"
  homepage "https://github.com/Kotlin/kotlin-interactive-shell"
  url "{{distributionUrl}}"
  version "{{projectVersion}}"
  sha256 "{{distributionSha256}}"
  license "Apache-2.0"

  bottle :unneeded

  depends_on "openjdk@8"

  def install
    libexec.install "bin", "lib"
    rm Dir["#{libexec}/bin/*.bat"]
    bin.install Dir["#{libexec}/bin/*"]
    bin.env_script_all_files libexec/"bin", Language::Java.overridable_java_home_env
  end

  test do
    output = shell_output("#{bin}/ki.sh <<< '' 2>&1 | grep ki-shell")
    assert_match "ki-shell 0.3/1.6.0", output
  end
end
