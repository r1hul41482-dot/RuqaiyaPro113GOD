#!/data/data/com.termux/files/usr/bin/bash
# Termux theke GitHub-e push korar script. Build GitHub Actions-e hoy.
# Usage: bash PUSH_TO_GITHUB_TERMUX.sh https://github.com/USERNAME/REPO.git
set -e
pkg install git -y
[ -d .git ] || git init
git add .
git commit -m "RuqaiyaPro update" || true
git branch -M main
git remote remove origin 2>/dev/null || true
git remote add origin "$1"
git push -u origin main
echo "Done. GitHub repo > Actions tab-e build dekho; shesh hole Artifacts theke APK nao."
