import { useState, useRef, useEffect, useCallback } from 'react'

function bootstrapMaps(apiKey) {
  if (typeof window.google?.maps?.importLibrary === 'function') return
  // prettier-ignore
  // eslint-disable-next-line
  ;(g=>{var h,a,k,p="The Google Maps JavaScript API",c="google",l="importLibrary",q="__ib__",m=document,b=window;b=b[c]||(b[c]={});var d=b.maps||(b.maps={}),r=new Set,e=new URLSearchParams,u=()=>h||(h=new Promise(async(f,n)=>{await(a=m.createElement("script"));e.set("libraries",[...r]+"");for(k in g)e.set(k.replace(/[A-Z]/g,t=>"_"+t[0].toLowerCase()),g[k]);e.set("callback",c+".maps."+q);a.src=`https://maps.googleapis.com/maps/api/js?`+e;d[q]=f;a.onerror=()=>h=n(Error(p+" could not load."));a.nonce=m.querySelector("script[nonce]")?.nonce||"";m.head.append(a)}));d[l]?console.warn(p+" only loads once. Ignoring:",g):d[l]=(f,...n)=>r.add(f)&&u().then(()=>d[l](f,...n))})({key:apiKey,v:"weekly"})
}

export default function PlacesAutocompleteInput({
  value,
  onChange,
  onPlaceSelect,
  placeholder = '15 Awolowo Road, Ikoyi, Lagos',
  required,
  className,
}) {
  const [suggestions, setSuggestions] = useState([])
  const libRef = useRef(null)
  const tokenRef = useRef(null)
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY

  const inputClass =
    className ??
    'w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-teal-600 focus:border-transparent bg-white'

  useEffect(() => {
    if (!apiKey) return
    bootstrapMaps(apiKey)
    window.google.maps
      .importLibrary('places')
      .then((lib) => { libRef.current = lib })
      .catch((err) => console.warn('[Places]', err.message))
  }, [])

  const fetchSuggestions = useCallback(async (input) => {
    if (!libRef.current || input.trim().length < 3) {
      setSuggestions([])
      return
    }
    try {
      const { AutocompleteSuggestion, AutocompleteSessionToken } = libRef.current
      if (!tokenRef.current) tokenRef.current = new AutocompleteSessionToken()
      const { suggestions: sugs } = await AutocompleteSuggestion.fetchAutocompleteSuggestions({
        input,
        sessionToken: tokenRef.current,
      })
      setSuggestions(
        sugs
          .filter(s => s.placePrediction?.text?.text)
          .map(s => ({ text: s.placePrediction.text.text, prediction: s.placePrediction }))
      )
    } catch {
      setSuggestions([])
    }
  }, [])

  const handleChange = (e) => {
    onChange(e.target.value)
    // Clear any previously resolved coordinates when the user edits manually
    if (onPlaceSelect) onPlaceSelect({ address: e.target.value, latitude: null, longitude: null })
    fetchSuggestions(e.target.value)
  }

  const handleSelect = async ({ text, prediction }) => {
    onChange(text)
    setSuggestions([])
    tokenRef.current = null

    if (!onPlaceSelect) return

    if (!prediction) {
      onPlaceSelect({ address: text, latitude: null, longitude: null })
      return
    }

    try {
      const place = prediction.toPlace()
      await place.fetchFields({ fields: ['location', 'formattedAddress'] })
      const lat = place.location?.lat() ?? null
      const lng = place.location?.lng() ?? null
      onPlaceSelect({
        address: place.formattedAddress || text,
        latitude: lat,
        longitude: lng,
      })
    } catch (err) {
      console.warn('[Places] Could not fetch coordinates:', err.message)
      onPlaceSelect({ address: text, latitude: null, longitude: null })
    }
  }

  return (
    <div className="relative">
      <input
        type="text"
        value={value}
        onChange={handleChange}
        onBlur={() => setTimeout(() => setSuggestions([]), 150)}
        placeholder={placeholder}
        required={required}
        autoComplete="off"
        className={inputClass}
      />
      {suggestions.length > 0 && (
        <ul className="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden">
          {suggestions.map((s, i) => (
            <li
              key={i}
              onMouseDown={() => handleSelect(s)}
              className="px-3 py-2.5 text-sm text-gray-700 hover:bg-teal-50 cursor-pointer border-b border-gray-100 last:border-b-0"
            >
              {s.text}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
