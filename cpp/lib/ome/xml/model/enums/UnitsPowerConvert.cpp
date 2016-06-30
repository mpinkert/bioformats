/*
 * #%L
 * OME-XML C++ library for working with OME-XML metadata structures.
 * %%
 * Copyright © 2016 Open Microscopy Environment:
 *   - Massachusetts Institute of Technology
 *   - National Institutes of Health
 *   - University of Dundee
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

#include <boost/preprocessor.hpp>

#include <ome/common/units/power.h>

#include <ome/xml/model/enums/UnitsPower.h>
#include <ome/xml/model/primitives/Quantity.h>

using ome::xml::model::enums::UnitsPower;

namespace
{

  using namespace ome::common::units;

  // For future use if portable (to replace the static property structs); tuples are enum name and unit quantity type
#define POWER_PROPERTY_LIST                                         \
  ((YOTTAWATT)(yottawatt_quantity))                                 \
  ((ZETTAWATT)(zettawatt_quantity))                                 \
  ((EXAWATT)(exawatt_quantity))                                     \
  ((PETAWATT)(petawatt_quantity))                                   \
  ((TERAWATT)(terawatt_quantity))                                   \
  ((GIGAWATT)(gigawatt_quantity))                                   \
  ((MEGAWATT)(megawatt_quantity))                                   \
  ((KILOWATT)(kilowatt_quantity))                                   \
  ((HECTOWATT)(hectowatt_quantity))                                 \
  ((DECAWATT)(decawatt_quantity))                                   \
  ((WATT)(watt_quantity))                                           \
  ((DECIWATT)(deciwatt_quantity))                                   \
  ((CENTIWATT)(centiwatt_quantity))                                 \
  ((MILLIWATT)(milliwatt_quantity))                                 \
  ((MICROWATT)(microwatt_quantity))                                 \
  ((NANOWATT)(nanowatt_quantity))                                   \
  ((PICOWATT)(picowatt_quantity))                                   \
  ((FEMTOWATT)(femtowatt_quantity))                                 \
  ((ATTOWATT)(attowatt_quantity))                                   \
  ((ZEPTOWATT)(zeptowatt_quantity))                                 \
  ((YOCTOWATT)(yoctowatt_quantity))
    
  /**
   * Map a given UnitsPower enum to the corresponding language types.
   */
  template<int>
  struct PowerProperties;
  
#define POWER_UNIT_CASE(maR, maProperty, maType)                       \
  template<>                                                            \
  struct PowerProperties<UnitsPower::BOOST_PP_SEQ_ELEM(0, maType)> \
  {                                                                     \
    typedef BOOST_PP_SEQ_ELEM(1, maType) quantity_type;                 \
  };

  BOOST_PP_SEQ_FOR_EACH(POWER_UNIT_CASE, %%, POWER_PROPERTY_LIST)

  // Convert two units
  template<typename Q, int Src, int Dest>
  Q
  convert_src_dest(typename Q::value_type v,
                   typename Q::unit_type dest)
  {
    typename PowerProperties<Dest>::quantity_type d(PowerProperties<Src>::quantity_type::from_value(v));
    return Q(quantity_cast<typename Q::value_type>(d), dest);
  }

// No switch default to avoid -Wunreachable-code errors.
// However, this then makes -Wswitch-default complain.  Disable
// temporarily.
#ifdef __GNUC__
#  pragma GCC diagnostic push
#  pragma GCC diagnostic ignored "-Wswitch-default"
#endif

#define DEST_UNIT_CASE(maR, maProperty, maType)                         \
  case UnitsPower::maType:                                           \
  {                                                                     \
    maProperty = convert_src_dest<Q, Src, UnitsPower::maType>(value, dest); \
  }                                                                     \
  break;

  template<typename Q, int Src>
  Q
  convert_dest(typename Q::value_type value,
               typename Q::unit_type dest)
  {
    Q q;

    switch(dest)
      {
        BOOST_PP_SEQ_FOR_EACH(DEST_UNIT_CASE, q, OME_XML_MODEL_ENUMS_UNITSPOWER_VALUES);
      }

    return q;
  }

#undef DEST_UNIT_CASE

#ifdef __GNUC__
#  pragma GCC diagnostic pop
#endif

}

namespace ome
{
  namespace xml
  {
    namespace model
    {
      namespace primitives
      {
        
        // No switch default to avoid -Wunreachable-code errors.
        // However, this then makes -Wswitch-default complain.  Disable
        // temporarily.
#ifdef __GNUC__
#  pragma GCC diagnostic push
#  pragma GCC diagnostic ignored "-Wswitch-default"
#endif

#define SRC_UNIT_CASE(maR, maProperty, maType)                          \
        case UnitsPower::maType:                                     \
          maProperty = convert_dest<Quantity<UnitsPower>, UnitsPower::maType>(quantity.getValue(), unit); \
          break;

        Quantity<UnitsPower>
        convert(const Quantity<UnitsPower>&     quantity,
                Quantity<UnitsPower>::unit_type unit)
        {
          Quantity<UnitsPower> q;

          switch(quantity.getUnit())
            {
              BOOST_PP_SEQ_FOR_EACH(SRC_UNIT_CASE, q, OME_XML_MODEL_ENUMS_UNITSPOWER_VALUES);
            }

          return q;
        }

#undef SRC_UNIT_CASE

#ifdef __GNUC__
#  pragma GCC diagnostic pop
#endif

      }
    }
  }
}
