# 📤 Как загрузить Android проект на GitHub

## Через GitHub Desktop (ПРОЩЕ ВСЕГО)

1. **Откройте GitHub Desktop**

2. **File → Add Local Repository**
   - Выберите папку `BluetoothMessengerAndroid`
   - Если появится "not a git repository" - нажмите "Create a repository"

3. **Сделайте коммит:**
   - Все файлы должны быть отмечены галочками
   - Summary: `Initial commit: Android Bluetooth Messenger`
   - Нажмите "Commit to main"

4. **Опубликуйте:**
   - Нажмите "Publish repository"
   - Name: `BluetoothMessengerAndroid`
   - Нажмите "Publish repository"

5. **Готово!**
   - Откройте https://github.com/worsed/BluetoothMessengerAndroid
   - Вкладка "Actions"
   - Через 5-10 минут APK будет готов!

## Через командную строку

```bash
cd BluetoothMessengerAndroid

# Инициализация
git init

# Добавить все файлы
git add .

# Коммит
git commit -m "Initial commit: Android Bluetooth Messenger"

# Подключить GitHub
git remote add origin https://github.com/worsed/BluetoothMessengerAndroid.git

# Загрузить
git branch -M main
git push -u origin main
```

## ⚠️ Важно!

Убедитесь что загружены ВСЕ файлы, особенно:
- ✅ `gradlew` (без расширения)
- ✅ `gradlew.bat`
- ✅ `gradle/wrapper/gradle-wrapper.properties`
- ✅ `.github/workflows/build-android.yml`

## После загрузки

1. GitHub Actions запустится автоматически
2. Через 5-10 минут APK будет готов
3. Скачайте из Artifacts
4. Установите на телефон!

## Если ошибка "gradlew not found"

Значит файл `gradlew` не загрузился. Проверьте:

```bash
# В папке BluetoothMessengerAndroid должны быть:
ls -la gradlew
ls -la gradlew.bat
ls -la gradle/wrapper/
```

Если файлов нет - они не закоммичены. Повторите:
```bash
git add gradlew gradlew.bat gradle/
git commit -m "Add gradle wrapper"
git push
```
