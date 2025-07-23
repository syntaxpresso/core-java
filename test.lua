local handle = io.popen(
  './build/native/nativeCompile/core-java create-new-java-file --package-name="io.github.syntaxpresso.test" --file-name="Test.java" --file-type="CLASS" 2> /dev/null')
local json_output = handle:read('*a')
handle:close()

-- Simple check for success/error (assuming your JSON format)
if json_output:find('"success":true') then
  print("Command succeeded!")
  -- Extract payload between "payload":" and "}
  local payload_start = json_output:find('"payload":"') + 10
  local payload_end = json_output:find('"}', payload_start) - 1
  local template = json_output:sub(payload_start, payload_end)
  print("Template:\n" .. template)
else
  print("Command failed!")
  print("Raw output: " .. json_output)
end
