class {{brewFormulaName}} < Formula
  desc "ki shell"
  homepage "https://github.com/Kotlin/kotlin-interactive-shell"
  version "{{projectVersion}}"
  url "{{distributionUrl}}"
  sha256 "{{distributionSha256}}"
  license "Apache-2"

  bottle :unneeded

  depends_on "openjdk@8"

  def install
    libexec.install Dir["*"]
    rm Dir["#{libexec}/bin/*.bat"]
    bin.install_symlink "#{libexec}/bin/*"
  end

  test do
    output = shell_output("#{bin}/bin/ki.sh <<< '' 2>&1 | grep ki-shell")
    assert_match "ki-shell 0.3/1.4.32", output
  end
end
