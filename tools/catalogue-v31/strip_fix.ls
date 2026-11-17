-- Product Strip Fix Class (between Product Strip Base Class and Product Strip Default Class).
-- r31's Product Strip Base Class resolveMembers counts its products in i, and its loop over a deal's contents
-- ("repeat with i = 1 to tOffer.getCount()") reuses i: every furni download registered after a deal carries the wrong
-- #itemIndex, so when it completes the wrong slot (or none) is refreshed and the right one keeps the loading icon
-- (Trax: the jukebox after the sound machine deal; Start pakker: the chairs after seven deals).
-- Instead of the single slot, every completed download resolves the previews of all slots that still have none.
on downloadCompleted me, tProps
  if ilk(me.pStripData) <> #propList then
    return 0
  end if
  tCount = me.pStripData.count
  repeat with tIndex = 1 to tCount
    tGroup = me.pStripData[tIndex]
    if voidp(tGroup.getSmallPreview()) then
      if tGroup.getCount() > 0 then
        tPrev = me.resolveSmallPreview(tGroup.getOffer(1))
        if ilk(tPrev) = #image then
          tGroup.setSmallPreview(tPrev)
          me.renderStripItem(tIndex)
        end if
      end if
    end if
  end repeat
  return 1
end
