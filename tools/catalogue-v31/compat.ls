-- Framework handlers the v31 catalogue (hh_cat_new) and dynamic downloader use, for the older Habbo client.

on getStringVariable tID, tDefault
  tValue = getVariable(tID, tDefault)
  if symbolp(tValue) then
    return "#" & tValue
  end if
  return string(tValue)
end

on decodeUTF8 tStr, tForceDecode
  return tStr
end
